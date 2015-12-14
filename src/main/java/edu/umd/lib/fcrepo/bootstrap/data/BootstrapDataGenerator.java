package edu.umd.lib.fcrepo.bootstrap.data;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;

/**
 * The class generates rdf dataset parsing fedora 2 objects, which can be bootstrapped to the fedora 4 repository using
 * the fcrepo-sample-dataset project.
 * 
 * @author mohideen
 * 
 */

public class BootstrapDataGenerator {

  private final static Logger LOGGER = getLogger(BootstrapDataGenerator.class);

  private final static String COLLECTION = "UMD_COLLECTION";

  public static void main(String[] args) {
    LOGGER.info("Bootstrap Data Generator.");
    final String defaultPidsFile = BootstrapDataGenerator.class.getResource("/pid-list.txt").getFile();
    final String pidFile = System.getProperty("pids.list", defaultPidsFile);
    final String fedora2Url = System.getProperty("fedora2.url", "http://fedora.lib.umd.edu/fedora").concat("/get/");
    final String outDir = System.getProperty("out.dir", "./target/bootstrap-data");
    final PCDMGenerator pcdmGen = new PCDMGenerator(outDir);
    LOGGER.info("Processing pids from {}.", pidFile);
    LOGGER.info("Output directory {}.", outDir);

    try {
      final BufferedReader br = new BufferedReader(new FileReader(new File(pidFile)));
      String line;
      Fedora2Item item;
      while ((line = br.readLine()) != null) {
        try {
          LOGGER.info("Processing pid {}", line);
          item = new Fedora2Item(fedora2Url, line.trim());
          generateRDF(item, pcdmGen);
        } catch (Exception e) {
          LOGGER.warn("Exception while retrieving pid {}. Skipping!", line);
          LOGGER.debug("Exception:", e);
        }
      }
      br.close();
    } catch (IOException e) {
      LOGGER.error("Exception while accessing pid list file!");
      LOGGER.debug("Exception:", e);
      System.exit(1);
    }

  }

  private static void generateRDF(Fedora2Item item, PCDMGenerator pcdmGen) throws IOException {
    // Generate Parent
    if (!item.getContainingCollections().isEmpty()) {
      Iterator<Fedora2Item> parentIter = item.getContainingCollections().iterator();
      while (parentIter.hasNext()) {
        Fedora2Item parent = parentIter.next();
        generateRDF(parent, pcdmGen);
        pcdmGen.addMember(parent, item);
      }
    }

    // Generate Current
    LOGGER.trace("Processing {} of type {}", item.getPid(), item.getType());
    if (COLLECTION.equals(item.getType())) { // Collection
      pcdmGen.generateCollectionForUMDM(item);
    } else if (item.getType() != null) { // Non collection objects
      pcdmGen.generateForUMDM(item);
    }

    // Generate Children
    if (!item.getParts().isEmpty()) {
      Iterator<ItemPart> childIter = item.getParts().iterator();
      while (childIter.hasNext()) {
        pcdmGen.generateForPart(childIter.next(), item);
      }
    }
  }

}
