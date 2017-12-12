/*
 *
 * Copyright 2017-2018 549477611@qq.com(xiaoyu)
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.myth.core.service.handler;

import com.github.myth.common.bean.context.MythTransactionContext;
import com.github.myth.core.concurrent.threadlocal.TransactionContextLocal;
import com.github.myth.core.service.MythTransactionHandler;
import com.github.myth.core.service.impl.MythTransactionManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <p>Description: .</p>
 * Myth分布式事务参与者， 参与分布式事务的接口会进入该handler
 *
 * @author xiaoyu(Myth)
 * @version 1.0
 * @date 2017/11/30 10:11
 * @since JDK 1.8
 */
@Component
public class ActorMythTransactionHandler implements MythTransactionHandler {

    private static final Lock LOCK = new ReentrantLock();


    private final MythTransactionManager mythTransactionManager;

    @Autowired
    public ActorMythTransactionHandler(MythTransactionManager mythTransactionManager) {
        this.mythTransactionManager = mythTransactionManager;
    }


    /**
     * Myth分布式事务处理接口
     *
     * @param point                  point 切点
     * @param mythTransactionContext myth事务上下文
     * @return Object
     * @throws Throwable 异常
     */
    @Override
    public Object handler(ProceedingJoinPoint point, MythTransactionContext mythTransactionContext) throws Throwable {

        try {
            //处理并发问题
            LOCK.lock();
            //发起调用 执行try方法
            final Object proceed = point.proceed();

            //执行成功 记录日志信息，通过mq来执行

            mythTransactionManager.commitTransaction(point, mythTransactionContext);

            return proceed;

        } catch (Throwable throwable) {
            mythTransactionManager.failureTransaction(point, mythTransactionContext.getTransId(), throwable.getMessage());
            throw throwable;
        } finally {
            LOCK.unlock();
            TransactionContextLocal.getInstance().remove();

        }
    }
}
