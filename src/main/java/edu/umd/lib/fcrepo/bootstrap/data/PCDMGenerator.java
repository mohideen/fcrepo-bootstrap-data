package edu.umd.lib.fcrepo.bootstrap.data;

import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.DIRECT_CONTAINER_TTL;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.INDIRECT_CONTAINER_TTL;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.PCDM_COLLECTION;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.PCDM_FILE_RU;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.PCDM_OBJECT;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.PCDM_OBJECT_TTL;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.TTL_END;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.TTL_START;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.TTL_STATEMENT_SEP;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.getDCDescStatement;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.getDCIdStatement;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.getDCTitleStatement;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.getDCTypeStatement;
import static edu.umd.lib.fcrepo.bootstrap.data.RDFHelper.getIndirectProxyTtl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PCDMGenerator {

  final static Logger LOGGER = LoggerFactory.getLogger(PCDMGenerator.class);
  final String outDir;

  public PCDMGenerator(String outDir) {
    if (outDir.endsWith("/")) {
      this.outDir = outDir;
    } else {
      this.outDir = outDir + "/";
    }

    createCollectionObjectDirecotries();
    createInfoRDFs();
  }

  private void createInfoRDFs() {
    writeStringToPath(TTL_START + getDCTitleStatement("Bootstrapped Dataset") + TTL_END, outDir + "_.ttl");
    writeStringToPath(TTL_START + getDCTitleStatement("PCDM Collections") + TTL_END, outDir + "collections/_.ttl");
    writeStringToPath(TTL_START + getDCTitleStatement("PCDM Objects") + TTL_END, outDir + "objects/_.ttl");
  }

  private void writeStringToPath(String content, String path) {
    File file = new File(path);
    try {
      FileWriter fw = new FileWriter(file, false);
      fw.write(content);
      fw.close();
    } catch (Exception e) {
      LOGGER.warn("Exception occured while writing to {}.", path);
      LOGGER.debug("", e);
    }

  }

  private void createCollectionObjectDirecotries() {
    File collectionsDir = new File(outDir + "collections");
    File objectsDir = new File(outDir + "objects");
    if (!collectionsDir.exists()) {
      collectionsDir.mkdirs();
      LOGGER.trace("Creating collections dir!");
    }
    if (!objectsDir.exists()) {
      objectsDir.mkdirs();
      LOGGER.trace("Creating objects dir!");
    }

  }

  public void generateForPart(ItemPart part, Fedora2Item parent) throws IOException {
    LOGGER.info("Generating PCDM RDF for Part pid {}", part.getPid());
    String partPath = outDir + "objects/" + getNameForPid(part.getPid());
    File partDir = new File(partPath);
    partDir.mkdirs();
    writeStringToPath(PCDM_OBJECT_TTL, partPath + "/_.ttl");
    String partFilesPath = partPath + "/files";
    File partFilesDir = new File(partFilesPath);
    if (!partFilesDir.exists()) {
      partFilesDir.mkdirs();
      writeStringToPath(DIRECT_CONTAINER_TTL, partFilesPath + "/_.ttl");
    }
    String partFilePath;
    if (part.getType().equals(ItemPart.IMAGE)) {
      partFilePath = partFilesPath + "/" + part.getFileName();
      File partFile = new File(partFilePath);
      partFile.mkdirs();
      String extension = FilenameUtils.getExtension(partFilePath);
      // Download Image File
      URL website = new URL(part.getPartUrl() + "/image");
      ReadableByteChannel rbc = Channels.newChannel(website.openStream());
      FileOutputStream fos = new FileOutputStream(partFilePath + "/_." + extension);
      fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
      writeStringToPath(PCDM_FILE_RU, partFilePath + "/fcr:metadata.ru");
      fos.close();
    } else if (part.getType().equals(ItemPart.VIDEO)) {
      // Not yet implemented
    }

    // add membership to parent
    String parentMemberPath = outDir + "objects/" + getNameForPid(parent.getPid()) + "/members/";
    File parentMemberDir = new File(parentMemberPath);
    if (!parentMemberDir.exists()) {
      parentMemberDir.mkdirs();
      writeStringToPath(INDIRECT_CONTAINER_TTL, parentMemberPath + "_.ttl");
    }
    String memberName = getNameForPid(part.getPid());
    File emptyMemberTtlFile = new File(parentMemberPath + memberName + "Proxy.ttl");
    emptyMemberTtlFile.createNewFile();
    String memberPath = parentMemberPath + memberName + "Proxy.ru";
    writeStringToPath(getIndirectProxyTtl(memberName), memberPath);
  }

  public void generateForUMDM(Fedora2Item item) {
    LOGGER.trace("Generating PCDM RDF for UMDM pid {}", item.getPid());
    String objectPath = outDir + "objects/" + getNameForPid(item.getPid());
    File objectDir = new File(objectPath);
    objectDir.mkdirs();
    StringBuilder br = new StringBuilder();
    br.append(TTL_START);
    br.append(PCDM_OBJECT);
    br.append(TTL_STATEMENT_SEP);
    br.append(getMetaDataStatements(item));
    br.append(TTL_END);
    writeStringToPath(br.toString(), objectPath + "/_.ttl");
  }

  public void generateCollectionForUMDM(Fedora2Item item) {

    String collectionPath = outDir + "collections/" + getNameForPid(item.getPid());
    File collectionDir = new File(collectionPath);
    if (!collectionDir.exists()) {
      LOGGER.trace("Generating PCDM RDF for UMDM Collection pid {}", item.getPid());
      collectionDir.mkdirs();
      StringBuilder br = new StringBuilder();
      br.append(TTL_START);
      br.append(PCDM_COLLECTION);
      br.append(TTL_STATEMENT_SEP);
      br.append(getMetaDataStatements(item));
      br.append(TTL_END);
      writeStringToPath(br.toString(), collectionPath + "/_.ttl");
    } else {
      LOGGER.trace("UMDM Collection pid {} exists already!", item.getPid());
    }
  }

  private Object getMetaDataStatements(Fedora2Item item) {
    StringBuilder br = new StringBuilder();
    br.append(getDCIdStatement(item.getPid()));
    br.append(TTL_STATEMENT_SEP);
    if (!item.getTitle().isEmpty()) {
      br.append(getDCTitleStatement(item.getTitle()));
      br.append(TTL_STATEMENT_SEP);
    }
    if (!item.getType().isEmpty()) {
      br.append(getDCTypeStatement(item.getType()));
      br.append(TTL_STATEMENT_SEP);
    }
    if (!item.getSummary().isEmpty()) {
      br.append(getDCDescStatement(item.getSummary()));
    }
    return br.toString();
  }

  public void addMember(Fedora2Item parent, Fedora2Item item) throws IOException {
    LOGGER.trace("Generating PCDM RDF membership for {} UMDM Collection pid {}", item.getPid(), parent.getPid());
    String collectionMemberPath = outDir + "collections/" + getNameForPid(parent.getPid()) + "/members/";
    File collectionMemberDir = new File(collectionMemberPath);
    if (!collectionMemberDir.exists()) {
      collectionMemberDir.mkdirs();
      writeStringToPath(INDIRECT_CONTAINER_TTL, collectionMemberPath + "_.ttl");
    }
    String memberName = getNameForPid(item.getPid());
    File emptyMemberTtlFile = new File(collectionMemberPath + memberName + "Proxy.ttl");
    emptyMemberTtlFile.createNewFile();
    String memberPath = collectionMemberPath + memberName + "Proxy.ru";
    writeStringToPath(getIndirectProxyTtl(memberName), memberPath);
  }

  private String getNameForPid(String pid) {
    return pid.replaceAll(":", "");
  }
}
