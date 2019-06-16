package org.rockyang.mybatis.spring;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.cache.Cache;
import org.apache.ibatis.executor.ErrorContext;
import org.apache.ibatis.io.VFS;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.DatabaseIdProvider;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.wrapper.ObjectWrapperFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.rockyang.mybatis.plus.MybatisConfiguration;
import org.rockyang.mybatis.plus.MybatisXMLConfigBuilder;
import org.rockyang.mybatis.plus.typehandler.ConditionsTypeHandler;
import org.rockyang.mybatis.plus.typehandler.DateTypeHandler;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.NestedIOException;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import static org.springframework.util.Assert.notNull;
import static org.springframework.util.Assert.state;
import static org.springframework.util.ObjectUtils.isEmpty;
import static org.springframework.util.StringUtils.hasLength;
import static org.springframework.util.StringUtils.tokenizeToStringArray;

/**
 * SqlSessionFactoryBean 拓展
 * 迁移自 mybatis-spring 的 org.mybatis.spring.SqlSessionFactoryBean
 *
 * {@code FactoryBean} that creates an MyBatis {@code SqlSessionFactory}.
 * This is the usual way to set up a shared MyBatis {@code SqlSessionFactory} in a Spring application context;
 * the SqlSessionFactory can then be passed to MyBatis-based DAOs via dependency injection.
 *
 * Either {@code DataSourceTransactionManager} or {@code JtaTransactionManager} can be used for transaction
 * demarcation in combination with a {@code SqlSessionFactory}. JTA should be used for transactions
 * which span multiple databases or when container managed transactions (CMT) are being used.
 *
 * @author yangjian
 *
 * @see #setConfigLocation
 * @see #setDataSource
 * @version $Id$
 */
public class MybatisSqlSessionFactoryBean implements FactoryBean<SqlSessionFactory>, InitializingBean, ApplicationListener<ApplicationEvent> {

	protected static final Log LOGGER = LogFactory.getLog(MybatisSqlSessionFactoryBean.class);

	protected Resource configLocation;

	protected Configuration configuration;

	protected Resource[] mapperLocations;

	protected DataSource dataSource;

	protected TransactionFactory transactionFactory;

	protected Properties configurationProperties;

	protected SqlSessionFactoryBuilder sqlSessionFactoryBuilder = new SqlSessionFactoryBuilder();

	private SqlSessionFactory sqlSessionFactory;

	//EnvironmentAware requires spring 3.1
	protected String environment = org.mybatis.spring.SqlSessionFactoryBean.class.getSimpleName();

	private boolean failFast;

	protected Interceptor[] plugins;

	protected TypeHandler<?>[] typeHandlers;

	protected String typeHandlersPackage;

	protected Class<?>[] typeAliases;

	protected String typeAliasesPackage;

	protected Class<?> typeAliasesSuperType;

	//issue #19. No default provider.
	protected DatabaseIdProvider databaseIdProvider;

	protected Class<? extends VFS> vfs;

	protected Cache cache;

	protected ObjectFactory objectFactory;

	protected ObjectWrapperFactory objectWrapperFactory;

	/**
	 * Sets the ObjectFactory.
	 *
	 * @since 1.1.2
	 * @param objectFactory objectFactory
	 */
	public void setObjectFactory(ObjectFactory objectFactory) {
		this.objectFactory = objectFactory;
	}

	/**
	 * Sets the ObjectWrapperFactory.
	 *
	 * @since 1.1.2
	 * @param objectWrapperFactory objectWrapperFactory
	 */
	public void setObjectWrapperFactory(ObjectWrapperFactory objectWrapperFactory) {
		this.objectWrapperFactory = objectWrapperFactory;
	}

	/**
	 * Gets the DatabaseIdProvider
	 *
	 * @since 1.1.0
	 * @return DatabaseIdProvider
	 */
	public DatabaseIdProvider getDatabaseIdProvider() {
		return databaseIdProvider;
	}

	/**
	 * Sets the DatabaseIdProvider.
	 * As of version 1.2.2 this variable is not initialized by default.
	 *
	 * @since 1.1.0
	 * @param databaseIdProvider databaseIdProvider
	 */
	public void setDatabaseIdProvider(DatabaseIdProvider databaseIdProvider) {
		this.databaseIdProvider = databaseIdProvider;
	}

	public Class<? extends VFS> getVfs() {
		return this.vfs;
	}

	public void setVfs(Class<? extends VFS> vfs) {
		this.vfs = vfs;
	}

	public Cache getCache() {
		return this.cache;
	}

	public void setCache(Cache cache) {
		this.cache = cache;
	}

	/**
	 * Mybatis plugin list.
	 *
	 * @since 1.0.1
	 *
	 * @param plugins list of plugins
	 *
	 */
	public void setPlugins(Interceptor[] plugins) {
		this.plugins = plugins;
	}

	/**
	 * Packages to search for type aliases.
	 *
	 * @since 1.0.1
	 *
	 * @param typeAliasesPackage package to scan for domain objects
	 *
	 */
	public void setTypeAliasesPackage(String typeAliasesPackage) {
		this.typeAliasesPackage = typeAliasesPackage;
	}

	/**
	 * Super class which domain objects have to extend to have a type alias created.
	 * No effect if there is no package to scan configured.
	 *
	 * @since 1.1.2
	 *
	 * @param typeAliasesSuperType super class for domain objects
	 *
	 */
	public void setTypeAliasesSuperType(Class<?> typeAliasesSuperType) {
		this.typeAliasesSuperType = typeAliasesSuperType;
	}

	/**
	 * Packages to search for type handlers.
	 *
	 * @since 1.0.1
	 *
	 * @param typeHandlersPackage package to scan for type handlers
	 *
	 */
	public void setTypeHandlersPackage(String typeHandlersPackage) {
		this.typeHandlersPackage = typeHandlersPackage;
	}

	/**
	 * Set type handlers. They must be annotated with {@code MappedTypes} and optionally with {@code MappedJdbcTypes}
	 *
	 * @since 1.0.1
	 *
	 * @param typeHandlers Type handler list
	 */
	public void setTypeHandlers(TypeHandler<?>[] typeHandlers) {
		this.typeHandlers = typeHandlers;
	}

	/**
	 * List of type aliases to register. They can be annotated with {@code Alias}
	 *
	 * @since 1.0.1
	 *
	 * @param typeAliases Type aliases list
	 */
	public void setTypeAliases(Class<?>[] typeAliases) {
		this.typeAliases = typeAliases;
	}

	/**
	 * If true, a final check is done on Configuration to assure that all mapped
	 * statements are fully loaded and there is no one still pending to resolve
	 * includes. Defaults to false.
	 *
	 * @since 1.0.1
	 *
	 * @param failFast enable failFast
	 */
	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

	/**
	 * Set the location of the MyBatis {@code SqlSessionFactory} config file. A typical value is
	 * "WEB-INF/mybatis-configuration.xml".
	 * @param configLocation location of mybatis configure file
	 */
	public void setConfigLocation(Resource configLocation) {
		this.configLocation = configLocation;
	}

	/**
	 * Set a customized MyBatis configuration.
	 * @param configuration MyBatis configuration
	 * @since 1.3.0
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	/**
	 * Set locations of MyBatis mapper files that are going to be merged into the {@code SqlSessionFactory}
	 * configuration at runtime.
	 *
	 * This is an alternative to specifying "&lt;sqlmapper&gt;" entries in an MyBatis config file.
	 * This property being based on Spring's resource abstraction also allows for specifying
	 * resource patterns here: e.g. "classpath*:sqlmap/*-mapper.xml".
	 * @param mapperLocations mapper locations
	 */
	public void setMapperLocations(Resource[] mapperLocations) {
		this.mapperLocations = mapperLocations;
	}

	/**
	 * Set optional properties to be passed into the SqlSession configuration, as alternative to a
	 * {@code &lt;properties&gt;} tag in the configuration xml file. This will be used to
	 * resolve placeholders in the config file.
	 * @param sqlSessionFactoryProperties sqlSessionFactory properties
	 */
	public void setConfigurationProperties(Properties sqlSessionFactoryProperties) {
		this.configurationProperties = sqlSessionFactoryProperties;
	}

	/**
	 * Set the JDBC {@code DataSource} that this instance should manage transactions for. The {@code DataSource}
	 * should match the one used by the {@code SqlSessionFactory}: for example, you could specify the same
	 * JNDI DataSource for both.
	 *
	 * A transactional JDBC {@code Connection} for this {@code DataSource} will be provided to application code
	 * accessing this {@code DataSource} directly via {@code DataSourceUtils} or {@code DataSourceTransactionManager}.
	 *
	 * The {@code DataSource} specified here should be the target {@code DataSource} to manage transactions for, not
	 * a {@code TransactionAwareDataSourceProxy}. Only data access code may work with
	 * {@code TransactionAwareDataSourceProxy}, while the transaction manager needs to work on the
	 * underlying target {@code DataSource}. If there's nevertheless a {@code TransactionAwareDataSourceProxy}
	 * passed in, it will be unwrapped to extract its target {@code DataSource}.
	 * @param dataSource dataSource
	 */
	public void setDataSource(DataSource dataSource) {
		if (dataSource instanceof TransactionAwareDataSourceProxy) {
			// If we got a TransactionAwareDataSourceProxy, we need to perform
			// transactions for its underlying target DataSource, else data
			// access code won't see properly exposed transactions (i.e.
			// transactions for the target DataSource).
			this.dataSource = ((TransactionAwareDataSourceProxy) dataSource).getTargetDataSource();
		} else {
			this.dataSource = dataSource;
		}
	}

	/**
	 * Sets the {@code SqlSessionFactoryBuilder} to use when creating the {@code SqlSessionFactory}.
	 *
	 * This is mainly meant for testing so that mock SqlSessionFactory classes can be injected. By
	 * default, {@code SqlSessionFactoryBuilder} creates {@code DefaultSqlSessionFactory} instances.
	 * @param sqlSessionFactoryBuilder sqlSessionFactoryBuilder
	 */
	public void setSqlSessionFactoryBuilder(SqlSessionFactoryBuilder sqlSessionFactoryBuilder) {
		this.sqlSessionFactoryBuilder = sqlSessionFactoryBuilder;
	}

	/**
	 * Set the MyBatis TransactionFactory to use. Default is {@code SpringManagedTransactionFactory}
	 *
	 * The default {@code SpringManagedTransactionFactory} should be appropriate for all cases:
	 * be it Spring transaction management, EJB CMT or plain JTA. If there is no active transaction,
	 * SqlSession operations will execute SQL statements non-transactionally.
	 *
	 * <b>It is strongly recommended to use the default {@code TransactionFactory}.</b> If not used, any
	 * attempt at getting an SqlSession through Spring's MyBatis framework will throw an exception if
	 * a transaction is active.
	 *
	 * @see SpringManagedTransactionFactory
	 * @param transactionFactory the MyBatis TransactionFactory
	 */
	public void setTransactionFactory(TransactionFactory transactionFactory) {
		this.transactionFactory = transactionFactory;
	}

	/**
	 * <b>NOTE:</b> This class <em>overrides</em> any {@code Environment} you have set in the MyBatis
	 * config file. This is used only as a placeholder name. The default value is
	 * {@code SqlSessionFactoryBean.class.getSimpleName()}.
	 *
	 * @param environment the environment name
	 */
	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		notNull(dataSource, "Property 'dataSource' is required");
		notNull(sqlSessionFactoryBuilder, "Property 'sqlSessionFactoryBuilder' is required");
		state((configuration == null && configLocation == null) || !(configuration != null && configLocation != null),
				"Property 'configuration' and 'configLocation' can not specified with together");

		this.sqlSessionFactory = buildSqlSessionFactory();
	}

	/**
	 * Build a {@code SqlSessionFactory} instance.
	 *  这里扩展成自己的 SqlSessionFactory
	 * The default implementation uses the standard MyBatis {@code XMLConfigBuilder} API to build a
	 * {@code SqlSessionFactory} instance based on an Reader.
	 * Since 1.3.0, it can be specified a {@link Configuration} instance directly(without config file).
	 *
	 * @return SqlSessionFactory
	 * @throws IOException if loading the config file failed
	 *
	 * @author yangjian
	 */
	protected SqlSessionFactory buildSqlSessionFactory() throws IOException {

		Configuration configuration;
		// 这里替换成自定义的 config builder
		MybatisXMLConfigBuilder xmlConfigBuilder = null;
		if (this.configuration != null) {
			configuration = this.configuration;
			if (configuration.getVariables() == null) {
				configuration.setVariables(this.configurationProperties);
			} else if (this.configurationProperties != null) {
				configuration.getVariables().putAll(this.configurationProperties);
			}
		} else if (this.configLocation != null) {
			xmlConfigBuilder = new MybatisXMLConfigBuilder(this.configLocation.getInputStream(), null, this.configurationProperties);
			configuration = xmlConfigBuilder.getConfiguration();
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Property `configuration` or 'configLocation' not specified, using default MyBatis Configuration");
			}
			configuration = new MybatisConfiguration();
			configuration.setVariables(this.configurationProperties);
		}

		if (this.objectFactory != null) {
			configuration.setObjectFactory(this.objectFactory);
		}

		if (this.objectWrapperFactory != null) {
			configuration.setObjectWrapperFactory(this.objectWrapperFactory);
		}

		if (this.vfs != null) {
			configuration.setVfsImpl(this.vfs);
		}

		if (hasLength(this.typeAliasesPackage)) {
			String[] typeAliasPackageArray = tokenizeToStringArray(this.typeAliasesPackage,
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			for (String packageToScan : typeAliasPackageArray) {
				configuration.getTypeAliasRegistry().registerAliases(packageToScan,
						typeAliasesSuperType == null ? Object.class : typeAliasesSuperType);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Scanned package: '" + packageToScan + "' for aliases");
				}
			}
		}

		if (!isEmpty(this.typeAliases)) {
			for (Class<?> typeAlias : this.typeAliases) {
				configuration.getTypeAliasRegistry().registerAlias(typeAlias);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Registered type alias: '" + typeAlias + "'");
				}
			}
		}

		configuration.getTypeAliasRegistry().registerAlias(DateTypeHandler.class);

		if (!isEmpty(this.plugins)) {
			for (Interceptor plugin : this.plugins) {
				configuration.addInterceptor(plugin);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Registered plugin: '" + plugin + "'");
				}
			}
		}

		if (hasLength(this.typeHandlersPackage)) {
			String[] typeHandlersPackageArray = tokenizeToStringArray(this.typeHandlersPackage,
					ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);
			for (String packageToScan : typeHandlersPackageArray) {
				configuration.getTypeHandlerRegistry().register(packageToScan);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Scanned package: '" + packageToScan + "' for type handlers");
				}
			}
		}

		if (!isEmpty(this.typeHandlers)) {
			for (TypeHandler<?> typeHandler : this.typeHandlers) {
				configuration.getTypeHandlerRegistry().register(typeHandler);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Registered type handler: '" + typeHandler + "'");
				}
			}
		}
		// 默认加一个 conditions 的 typehandler
		configuration.getTypeHandlerRegistry().register(new ConditionsTypeHandler());

		if (this.databaseIdProvider != null) {//fix #64 set databaseId before parse mapper xmls
			try {
				configuration.setDatabaseId(this.databaseIdProvider.getDatabaseId(this.dataSource));
			} catch (SQLException e) {
				throw new NestedIOException("Failed getting a databaseId", e);
			}
		}

		if (this.cache != null) {
			configuration.addCache(this.cache);
		}

		if (xmlConfigBuilder != null) {
			try {
				xmlConfigBuilder.parse();
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Parsed configuration file: '" + this.configLocation + "'");
				}
			} catch (Exception ex) {
				throw new NestedIOException("Failed to parse config resource: " + this.configLocation, ex);
			} finally {
				ErrorContext.instance().reset();
			}
		}

		if (this.transactionFactory == null) {
			this.transactionFactory = new SpringManagedTransactionFactory();
		}

		configuration.setEnvironment(new Environment(this.environment, this.transactionFactory, this.dataSource));

		if (!isEmpty(this.mapperLocations)) { // 这里添加mapper
			for (Resource mapperLocation : this.mapperLocations) {
				if (mapperLocation == null) {
					continue;
				}
				try {
					XMLMapperBuilder xmlMapperBuilder = new XMLMapperBuilder(mapperLocation.getInputStream(),
							configuration, mapperLocation.toString(), configuration.getSqlFragments());
					xmlMapperBuilder.parse();
				} catch (Exception e) {
					throw new NestedIOException("Failed to parse mapping resource: '" + mapperLocation + "'", e);
				} finally {
					ErrorContext.instance().reset();
				}

				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Parsed mapper file: '" + mapperLocation + "'");
				}
			}
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Property 'mapperLocations' was not specified or no matching resources found");
			}
		}

		return this.sqlSessionFactoryBuilder.build(configuration);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SqlSessionFactory getObject() throws Exception {
		if (this.sqlSessionFactory == null) {
			afterPropertiesSet();
		}

		return this.sqlSessionFactory;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<? extends SqlSessionFactory> getObjectType() {
		return this.sqlSessionFactory == null ? SqlSessionFactory.class : this.sqlSessionFactory.getClass();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isSingleton() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (failFast && event instanceof ContextRefreshedEvent) {
			// fail-fast -> check all statements are completed
			this.sqlSessionFactory.getConfiguration().getMappedStatementNames();
		}
	}

	public Resource getConfigLocation() {
		return configLocation;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Resource[] getMapperLocations() {
		return mapperLocations;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public TransactionFactory getTransactionFactory() {
		return transactionFactory;
	}

	public Properties getConfigurationProperties() {
		return configurationProperties;
	}

	public SqlSessionFactoryBuilder getSqlSessionFactoryBuilder() {
		return sqlSessionFactoryBuilder;
	}

	public SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	}

	public String getEnvironment() {
		return environment;
	}

	public boolean isFailFast() {
		return failFast;
	}

	public Interceptor[] getPlugins() {
		return plugins;
	}

	public TypeHandler<?>[] getTypeHandlers() {
		return typeHandlers;
	}

	public String getTypeHandlersPackage() {
		return typeHandlersPackage;
	}

	public Class<?>[] getTypeAliases() {
		return typeAliases;
	}

	public String getTypeAliasesPackage() {
		return typeAliasesPackage;
	}

	public Class<?> getTypeAliasesSuperType() {
		return typeAliasesSuperType;
	}

	public ObjectFactory getObjectFactory() {
		return objectFactory;
	}

	public ObjectWrapperFactory getObjectWrapperFactory() {
		return objectWrapperFactory;
	}
}
