package com.dayi.mybatis.spring.plus.support.ext;

import com.dayi.mybatis.spring.plus.support.Criterion;

import java.util.*;

/**
 * @author chenzhaoju
 */
public class Junction implements Criterion {
    private final Nature nature;
    private final List<Criterion> conditions = new ArrayList<Criterion>();

    protected Junction(Nature nature) {
        this.nature = nature;
    }

    protected Junction(Nature nature, Criterion... criterion) {
        this(nature);
        Collections.addAll(conditions, criterion);
    }

    protected Junction(Nature nature, Collection<Criterion> criterions) {
        this(nature);
        this.conditions.addAll(criterions);
    }

    public Junction add(Criterion criterion) {
        conditions.add(criterion);
        return this;
    }

    public Nature getNature() {
        return nature;
    }

    public List<Criterion> conditions() {
        return conditions;
    }

    @Override
    public String toSqlString() {
        if (conditions.size() == 0) {
            return "1=1";
        }

        final StringBuilder buffer = new StringBuilder();
        if(1 < conditions.size()){
            buffer.append('(');
        }
        final Iterator itr = conditions.iterator();
        while (itr.hasNext()) {
            buffer.append(((Criterion) itr.next()).toSqlString());
            if (itr.hasNext()) {
                buffer.append(' ')
                        .append(nature.getOperator())
                        .append(' ');
            }
        }
        if(1 < conditions.size()){
            buffer.append(')');
        }
        return buffer.toString();
    }

    /**
     * The type of junction
     */
    public static enum Nature {
        /**
         * An AND
         */
        AND,
        /**
         * An OR
         */
        OR;

        /**
         * The corresponding SQL operator
         *
         * @return SQL operator
         */
        public String getOperator() {
            return name().toLowerCase();
        }
    }

}
