package edu.umd.lib.fcrepo.bootstrap.data;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * The class generates turtle files by parsing fedora 2 objects, which can be bootstrapped to the fedora 4 repository
 * using the fcrepo-sample-dataset project.
 * 
 * @author mohideen
 * 
 */

public class BootstrapDataGenerator {

  private final static Logger LOGGER = getLogger(BootstrapDataGenerator.class);

  public static void main(String[] args) {
    LOGGER.info("Bootstrap Data Generator.");
  }

}
