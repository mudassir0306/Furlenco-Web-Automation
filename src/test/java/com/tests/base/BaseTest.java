package com.tests.base;

import com.framework.config.ConfigManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Common ancestor for test base classes — holds config access and the logger.
 */
public abstract class BaseTest {

    protected static final Logger LOGGER = LogManager.getLogger(BaseTest.class);
    protected final ConfigManager config = ConfigManager.getInstance();
}
