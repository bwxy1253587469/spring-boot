/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import org.apache.commons.logging.Log;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A collection of {@link SpringApplicationRunListener}.
 *
 * @author Phillip Webb
 */
class SpringApplicationRunListeners {

	private final Log log;

	private final List<SpringApplicationRunListener> listeners;

	SpringApplicationRunListeners(Log log,
			Collection<? extends SpringApplicationRunListener> listeners) {
		this.log = log;
		this.listeners = new ArrayList<SpringApplicationRunListener>(listeners);
	}

	public void starting() {
		// 发布ApplicationStartedEvent事件
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.starting();
		}
	}

	/**
	 * ParentContextCloserApplicationListener
	 * 容器关闭时发出通知，如果父容器关闭，那么自容器也一起关闭
	 *
	 * FileEncodingApplicationListener（参数spring.mandatory-file-encoding）
	 * 在springboot环境准备完成以后运行，获取环境中的系统环境参数，检测当前系统环境的file.encoding和spring.mandatory-file-encoding设置的值是否一样,如果不一样则抛出异常
	 * 如果不配置spring.mandatory-file-encoding则不检查
	 *
	 * AnsiOutputApplicationListener（参数spring.output.ansi.enabled）
	 * 在springboot环境准备完成以后运行，
	 * 如果你的终端支持ANSI，设置彩色输出会让日志更具可读性。
	 *
	 * ConfigFileApplicationListener
	 * 重要（读取加载springboot配置文件）
	 *
	 * DelegatingApplicationListener（参数context.listener.classes）
	 * 可以在配置文件中指定ApplicationListener的实现类 https://www.jb51.net/article/124391.htm
	 * 把Listener转发给配置的这些class处理，这样可以支持外围代码不去写spring.factories中的org.springframework.context.ApplicationListener相关配置，保持springboot原来代码的稳定
	 *
	 * LiquibaseServiceLocatorApplicationListener（参数liquibase.servicelocator.ServiceLocator）
	 * 如果存在，则使用springboot相关的版本进行替代
	 *
	 * ClasspathLoggingApplicationListener
	 * 程序启动时，讲classpath打印到debug日志，启动失败时classpath打印到info日志
	 * LoggingApplicationListener
	 * 根据配置初始化日志系统log
	 *
	 * org.springframework.boot.autoconfigure.BackgroundPreinitializer
	 * 触发更早的初始化 异步运行
	 * ---------------------
	 * 作者：oldflame-Jm
	 * 来源：CSDN
	 * 原文：https://blog.csdn.net/jamet/article/details/78042486
	 * 版权声明：本文为博主原创文章，转载请附上博文链接！
	 * @param environment
	 */
	public void environmentPrepared(ConfigurableEnvironment environment) {
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.environmentPrepared(environment);
		}
	}

	public void contextPrepared(ConfigurableApplicationContext context) {
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.contextPrepared(context);
		}
	}

	public void contextLoaded(ConfigurableApplicationContext context) {
		for (SpringApplicationRunListener listener : this.listeners) {
			listener.contextLoaded(context);
		}
	}

	public void finished(ConfigurableApplicationContext context, Throwable exception) {
		for (SpringApplicationRunListener listener : this.listeners) {
			callFinishedListener(listener, context, exception);
		}
	}

	private void callFinishedListener(SpringApplicationRunListener listener,
			ConfigurableApplicationContext context, Throwable exception) {
		try {
			listener.finished(context, exception);
		}
		catch (Throwable ex) {
			if (exception == null) {
				ReflectionUtils.rethrowRuntimeException(ex);
			}
			if (this.log.isDebugEnabled()) {
				this.log.error("Error handling failed", ex);
			}
			else {
				String message = ex.getMessage();
				message = (message != null) ? message : "no error message";
				this.log.warn("Error handling failed (" + message + ")");
			}
		}
	}

}
