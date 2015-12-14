package edu.umd.lib.fcrepo.bootstrap.data;

public class ItemPart {

  public final static String IMAGE = "images";
  public final static String VIDEO = "videos";

  private String pid;
  private String fileId;
  private String fileName;
  private String partUrl;
  private String type;
  private String order;
  private String label;

  public String getPid() {
    return this.pid;
  }

  public String getFileId() {
    return this.fileId;
  }

  public String getFileName() {
    return this.fileName;
  }

  public String getPartUrl() {
    return this.partUrl;
  }

  public String getType() {
    return this.type;
  }

  public String getOrder() {
    return this.order;
  }

  public String getLabel() {
    return this.label;
  }

  public void setPid(String pid) {
    this.pid = pid;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public void setPartUrl(String partUrl) {
    this.partUrl = partUrl;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setOrder(String order) {
    this.order = order;
  }

  public void setLabel(String label) {
    this.label = label;
  }
}
