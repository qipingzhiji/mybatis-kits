package com.aizone.mybatis.spring.plus.support.ext;

import com.aizone.mybatis.spring.plus.support.Criterion;

/**
 * @author yangjian
 */
public class NotNullExpression  extends AbstractCriterion  implements Criterion {

    private final String propertyName;

    protected NotNullExpression(String propertyName) {
        this.propertyName = propertyName;
    }

    @Override
    public String toSqlString() {
        StringBuilder fragment = new StringBuilder();
        fragment.append(this.propertyName);
        fragment.append(" is not null ");
        return fragment.toString();
    }

    @Override
    public String toString() {
        return propertyName + " is not null";
    }
}