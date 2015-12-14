package edu.umd.lib.fcrepo.bootstrap.data;

import java.text.MessageFormat;

public class RDFHelper {

  public final static String TTL_START = "<> ";
  public final static String TTL_END = " .";

  public final static String TTL_STATEMENT_SEP = " ;\n";
  public final static String TTL_LIST_SEP = ",";

  public final static String PCDM_COLLECTION = "a pcdm:Collection";
  public final static String PCDM_OBJECT = "a pcdm:Object";
  public final static String PCDM_FILE = "a pcdm:File";

  public final static String PCDM_OBJECT_TTL = TTL_START + PCDM_OBJECT + TTL_END;

  public final static String INDIRECT_CONTAINER_TTL =
      "<> a ldp:IndirectContainer ;\n"
          + "ldp:membershipResource <.> ;\n"
          + "ldp:hasMemberRelation pcdm:hasMember ;\n"
          + "ldp:insertedContentRelation ore:proxyFor .";

  public final static String DIRECT_CONTAINER_TTL =
      "<> a ldp:DirectContainer ;\n"
          + "ldp:membershipResource <.> ;\n"
          + "ldp:hasMemberRelation pcdm:hasMember .";

  public final static String PCDM_FILE_RU =
      "INSERT {\n"
          + "  <> a pcdm:File .\n"
          + "} WHERE {}";

  private final static String MEMBER_PROXY_RU =
      "INSERT '{'\n"
          + "<> ore:proxyFor <../../../objects/{0}> ;\n"
          + "ore:proxyIn  <../> .\n"
          + "'}'\n"
          + "WHERE '{}'";

  public static String getStatement(String dcProperty, String title) {
    return dcProperty + " " + quoteAndEscape(title);
  }

  public static String getDCTitleStatement(String title) {
    return getStatement("dc:title", title);
  }

  public static String getDCTypeStatement(String type) {
    return getStatement("dc:type", type);
  }

  public static String getDCDescStatement(String summary) {
    return getStatement("dc:description", summary);
  }

  public static String getDCIdStatement(String pid) {
    return getStatement("dc:identifier", pid);
  }

  private static String quoteAndEscape(String string) {
    return "\"" + string.replaceAll("\"", "\\\\\"") + "\"";
  }

  public static String getIndirectProxyTtl(String memberName) {
    return MessageFormat.format(MEMBER_PROXY_RU, memberName);
  }

}
