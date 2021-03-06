package org.rockyang.mybatis.springboot.demo.test.plus;

import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.Before;
import org.junit.Test;
import org.rockyang.mybatis.springboot.demo.test.AbstractMybatis;
import org.rockyang.mybatis.springboot.demo.test.support.mapper.UserMapper;
import org.rockyang.mybatis.springboot.demo.test.support.model.User;
import org.rockyang.mybatis.plus.MybatisConfiguration;
import org.rockyang.mybatis.plus.support.Conditions;
import org.rockyang.mybatis.plus.support.ext.Restrictions;
import org.rockyang.mybatis.spring.MybatisSqlSessionFactoryBean;

import javax.sql.DataSource;

/**
 * <p>
 * 全局配置测试
 * </p>
 *
 * @author chenzhaoju
 * @author yangjian
 */
public class MappedStatementTest extends AbstractMybatis {

	private Configuration configuration ;

	@Before
	public void buildSessionFactory() throws Exception {
		MybatisSqlSessionFactoryBean builder = new MybatisSqlSessionFactoryBean();

		Configuration configuration = new MybatisConfiguration();
		DataSource dataSource = getDataSource();
		TransactionFactory transactionFactory = new JdbcTransactionFactory();

		Environment environment = new Environment("development", transactionFactory, dataSource);
		configuration.setEnvironment(environment);

		configuration.setLazyLoadingEnabled(true);
		/**
		 * 该配置直接开启缓存 ,默认是开启的
		 * 特别注意，这里是 Mybatis 的一个 Bug，如果设置为 false 会导致插件注入失效
		 * 直接的结果就是分页插件失效，物理分页不可用
		 */
		configuration.setCacheEnabled(true);
		/**
		 * 配置默认的执行器。SIMPLE 就是普通的执行器；
		 * REUSE 执行器会重用预处理语句（prepared statements）；
		 * BATCH 执行器将重用语句并执行批量更新;
		 */
		//configuration.setDefaultExecutorType(ExecutorType.REUSE) ;
		configuration.setDefaultExecutorType(ExecutorType.SIMPLE) ;

		configuration.getTypeAliasRegistry().registerAlias(User.class);
		configuration.addMapper(UserMapper.class);

		// spring 的步骤
		builder.setDataSource(dataSource);
		builder.setConfiguration(configuration);
		builder.afterPropertiesSet();
		this.configuration = configuration;
	}

	@Test
	public void testGetMapperStatement()
	{
		MappedStatement mappedStatement = configuration.getMappedStatement("org.rockyang.mybatis.boot.demo.test.support.mapper.UserMapper.getUserByName");
		System.out.println(mappedStatement);
		BoundSql boundSql = mappedStatement.getBoundSql("34234234");
		String sql = boundSql.getSql();
		System.out.println(sql);
	}

	@Test
	public void testSearchByConditionsMapper()
	{
		Conditions conditions = new Conditions();
		//conditions.add(Restrictions.eq("name", "名字"));
		conditions.add(Restrictions.or(Restrictions.like("name", "名字"),Restrictions.and(Restrictions.eq("count", 1),Restrictions.eq("count", 2))));

		org.apache.ibatis.mapping.MappedStatement mappedStatement = configuration.getMappedStatement("org.rockyang.mybatis.boot.demo.test.support.mapper.UserMapper.searchByConditions");

		BoundSql boundSql = mappedStatement.getBoundSql(conditions);
		String sql = boundSql.getSql();
		System.out.println(sql);
	}
}
