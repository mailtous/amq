package com.artlongs.amq.core.store;

import com.artlongs.amq.core.Subscribe;
import com.trigersoft.jaque.expression.*;
import org.osgl.util.C;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Func : 包装 lambda 条件表达式,并进行解析
 *
 * @author: leeton on 2019/3/26.
 */
public class Condition<T> {

    private Predicate<T> predicate; // 原表达式
    private Object val; //表达式的真正的值,eg: s -> s.getTopic().startsWith("hello") ,则 val = hello

    public static final Map<String, List<Integer>> hashMapOfTopic = new ConcurrentHashMap<>(); //以 dbName+topic 为 KEY 对应在的 HASH 列表

    /**
     * 条件表达式的代理接口
     **/
    public interface P<T> extends Predicate<T>, Serializable {
    }

    public Condition(P<T> predicate) {
        this.predicate = predicate;
        parse(predicate);
    }

    private Condition parse(P<T> propertyRef) {
        Expression expression = LambdaExpression.parse(propertyRef).getBody();
        if(expression instanceof InvocationExpression){
            List<Expression> expressionList= ((InvocationExpression) expression).getArguments();
            Object val = ((ConstantExpression) expressionList.get(0)).getValue();
            this.val = val;
        }else if(expression instanceof BinaryExpression) {// 二元表达式
            Expression exp = ((BinaryExpression) expression).getSecond();
            InvocableExpression member = ((InvocationExpression) exp).getTarget();
            this.val = member.toString();
        }
        return this;
    }

    public static int hashCondition(String dbName, Condition topicFilter, Condition timeFilter) {
        Object topic =topicFilter.val;
        String conditon = dbName + topic + timeFilter.val;
        int hash = additiveHash(conditon, 31);
        // push to cache
        List<Integer> hashList = hashMapOfTopic.get(dbName + topic);
        if (C.isEmpty(hashList)) {
            hashList = C.newList();
            hashList.add(hash);
            hashMapOfTopic.put(dbName + topic, hashList);
        } else {
            hashList.add(hash);
        }
        return hash;

    }

    public static int additiveHash(String key, int prime) {
        int hash, i;
        for (hash = key.length(), i = 0; i < key.length(); i++) {
            hash += key.charAt(i);
        }
        return (hash % prime);
    }

    public Predicate<T> getPredicate() {
        return predicate;
    }

    public Object getVal() {
        return val;
    }

    public static void main(String[] args) {
        Condition<Subscribe> c = new Condition<Subscribe>(s -> s.getTopic().startsWith("hello"));
        System.err.println(c.val);

        Condition<Subscribe> c2 = new Condition<Subscribe>(s -> s.getCtime() >= System.currentTimeMillis());
        System.err.println(c2.val);
    }


}
