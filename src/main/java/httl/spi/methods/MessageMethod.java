/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package httl.spi.methods;

import httl.Engine;
import httl.Resource;
import httl.spi.Logger;
import httl.spi.Resolver;
import httl.util.EncodingProperties;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * MessageMethod. (SPI, Singleton, ThreadSafe)
 * 
 * @author Liang Fei (liangfei0201 AT gmail DOT com)
 */
public class MessageMethod {

    private Engine engine;

	private Resolver resolver;

	private Logger logger;

	private String messageBasename;
	
	private String messageFormat;

	private String messageEncoding;
	
	private String messageSuffix;

	private boolean reloadable;

    /**
     * httl.properties: engine=httl.spi.engines.DefaultEngine
     */
    public void setEngine(Engine engine) {
        this.engine = engine;
    }

	/**
	 * httl.properties: reloadable=true
	 */
	public void setReloadable(boolean reloadable) {
		this.reloadable = reloadable;
	}

	/**
	 * httl.properties: message.encoding=UTF-8
	 */
	public void setMessageEncoding(String messageEncoding) {
		this.messageEncoding = messageEncoding;
	}

	/**
	 * httl.properties: message.suffix=.properties
	 */
	public void setMessageSuffix(String messageSuffix) {
		this.messageSuffix = messageSuffix;
	}

	/**
	 * httl.properties: message.basename=messages
	 */
	public void setMessageBasename(String messageBasename) {
		this.messageBasename = messageBasename;
	}

	/**
	 * httl.properties: message.format=string
	 */
	public void setMessageFormat(String messageFormat) {
		if (! "string".equals(messageFormat)
				&& ! "message".equals(messageFormat)) {
			throw new IllegalArgumentException("Unsupported message.format=" + messageFormat + ", only supported \"string\" or \"message\" format.");
		}
		this.messageFormat = messageFormat;
	}

	/**
	 * httl.properties: resolver=httl.spi.resolvers.EngineResolver
	 */
    public void setResolver(Resolver resolver) {
		this.resolver = resolver;
	}

    /**
	 * httl.properties: resolver=httl.spi.loggers.Log4jLogger
	 */
    public void setLogger(Logger logger) {
		this.logger = logger;
	}

	private String getLocale() {
		Object value = resolver.get("locale");
		return value == null ? null : value.toString();
	}

    public String message(String key) {
    	return message(key, null, new String[0]);
    }

    public String message(String key, Object arg0) {
    	return message(key, null, new Object[] {arg0});
    }

    public String message(String key, Object arg0, Object arg1) {
    	return message(key, null, new Object[] {arg0, arg1});
    }

    public String message(String key, Object arg0, Object arg1, Object arg2) {
    	return message(key, null, new Object[] {arg0, arg1, arg2});
    }

    public String message(String key, Object arg0, Object arg1, Object arg2, Object arg3) {
    	return message(key, null, new Object[] {arg0, arg1, arg2, arg3});
    }

    public String message(String key, Object[] args) {
    	return message(key, null, args);
    }

    public String message(String key, Locale locale) {
    	return message(key, locale, new String[0]);
    }

    public String message(String key, Locale locale, Object arg0) {
    	return message(key, locale, new Object[] {arg0});
    }

    public String message(String key, Locale locale, Object arg0, Object arg1) {
    	return message(key, locale, new Object[] {arg0, arg1});
    }

    public String message(String key, Locale locale, Object arg0, Object arg1, Object arg2) {
    	return message(key, locale, new Object[] {arg0, arg1, arg2});
    }

    public String message(String key, Locale locale, Object arg0, Object arg1, Object arg2, Object arg3) {
    	return message(key, locale, new Object[] {arg0, arg1, arg2, arg3});
    }

    public String message(String key, Locale locale, Object[] args) {
    	if (key == null || key.length() == 0 || messageBasename == null) {
    		return key;
    	}
    	String localeStr;
		if (locale != null) {
			localeStr = "_" + locale.toString();
		} else {
			localeStr = getLocale();
			if (localeStr == null) {
				localeStr = "";
			} else {
				localeStr = "_" + localeStr;
			}
		}
		String value = findMessageByLocale(localeStr, key);
		if (value != null && value.length() > 0) {
			if (args != null && args.length > 0) {
				if ("string".equals(messageFormat)) {
					return String.format(value, args);
				} else {
					return MessageFormat.format(value, args);
				}
			} else {
				return value;
			}
		}
		return key;
    }

    private final ConcurrentMap<String, EncodingProperties> messageCache = new ConcurrentHashMap<String, EncodingProperties>();

    private String findMessageByLocale(String locale, String key) {
    	String file = messageBasename + locale + messageSuffix;
    	EncodingProperties properties = messageCache.get(file);
		if ((properties == null || reloadable) && engine.hasResource(file)) {
			if (properties == null) {
				properties = new EncodingProperties();
				EncodingProperties old = messageCache.putIfAbsent(file, properties);
				if (old != null) {
					properties = old;
				}
			}
			try {
				Resource resource = engine.getResource(file);
				if (properties.getLastModified() < resource.getLastModified()) {
					String encoding = (messageEncoding == null || messageEncoding.length() == 0 ? "UTF-8" : messageEncoding);
					properties.load(resource.getInputStream(), encoding, resource.getLastModified());
				}
			} catch (IOException e) {
				if (logger != null && logger.isErrorEnabled()) {
					logger.error("Failed to load httl message message file " + file + ", cause: " + e.getMessage(), e);
				}
			}
		}
		if (properties != null) {
			String value = properties.getProperty(key);
			if (value != null && value.length() > 0) {
				return value;
			}
		}
    	if (locale.length() > 0) {
	    	int i = locale.lastIndexOf('_');
			if (i >= 0) {
				return findMessageByLocale(locale.substring(0, i), key);
			}
    	}
		return null;
    }

}