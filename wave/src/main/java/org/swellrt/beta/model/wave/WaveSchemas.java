package org.swellrt.beta.model.wave;

import java.util.Collections;

import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.schema.AbstractXmlSchemaConstraints;
import org.waveprotocol.wave.model.schema.SchemaUtils;

/**
 * Separated set of Wave document/blips schemas for use in SwellRT
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveSchemas {

  
  /**
   * Hard coded ("for now") conversation document schema constraints.
   * This is a copy of original Wave document schema.
   * TODO review and adapt to SwellRT needs
   */
  public static final DocumentSchema STEXT_SCHEMA_CONSTRAINTS = new TextDocumentSchema();

  public static class TextDocumentSchema extends AbstractXmlSchemaConstraints {
    {
      addChildren(null, "head");
      addChildren("head", "timestamp");
      addChildren("timestamp", "lmt");
      addAttrs("lmt", "t");

      addChildren(null, "body");

      lineContainer("body");

      addAttrWithValues("line", "t", "h1", "h2", "h3", "h4", "li");
      addAttrWithValues("line", "listyle", "decimal");
      addAttrWithValues("line", "a", "l", "r", "c", "j");
      addAttrWithValues("line", "d", "l", "r");
      // NOTE: for now, value constraints for indent implemented explicitly
      addAttrWithValues("line", "i");

      addChildren("image", "caption");
      addAttrWithValues("image", "attachment");
      addAttrWithValues("image", "style", "full");
      addChildren("image", "gadget");

      oneLiner("caption");
      addChildren("caption", "reply");
      oneLiner("label");
      oneLiner("input");

      addAttrs("reply", "id");

      containsFormElements("body");
      lineContainer("textarea");
      addAttrs("button", "name");
      addChildren("button", "caption", "events");
      addChildren("events", "click");
      addAttrs("click", "time", "clicker");
      addAttrs("check", "name", "submit", "value");
      addAttrs("radiogroup", "name", "submit", "value");
      addAttrs("password", "name", "submit", "value");
      addAttrs("textarea", "name", "submit", "value");
      addAttrs("input", "name", "submit");
      addAttrs("radio", "name", "group");
      addAttrs("click", "time", "clicker");
      addAttrs("label", "for");

      addChildren("gadget", "title", "thumbnail", "category", "state", "pref");
      // Some of these attributes might be obsolete and/or require stricter
      // validation
      addAttrs("gadget", "url", "title", "prefs", "state", "author", "height", "width", "id",
          "extension", "ifr", "snippet");
      for (String gadgetEl : new String[] {"category", "state", "pref"}) {
        addAttrs(gadgetEl, "name");
      }
      for (String gadgetEl : new String[] {"title", "thumbnail", "state", "pref"}) {
        addAttrs(gadgetEl, "value");
      }

      addChildren("profile", "profile-field", "gadget");
      addAttrs("profile-field", "name", "user-set");
      addAttrs("profile", "avatar-url");
      containsBlipText("profile-field");

      addChildren("mediasearch", "result", "customsearch");
      addAttrs("mediasearch", "page", "corpora", "query", "selected", "pending", "lang");
      addAttrs("result", "thumbnail", "thumbwidth", "thumbheight", "content", "url", "dispurl",
          "title", "snippet", "num", "type", "disphtml");
      addAttrs("customsearch", "name", "icon", "shortname", "resultrows", "resultcols",
          "addmethod");

      addChildren("body", "trustreq");
      containsBlipText("trustreq");
      addChildren("trustreq", "trwave");
      addAttrs("trustreq", "from", "numberOfWaves", "userAction");
      addAttrs("trwave", "messageCount", "lastModified");
      containsBlipText("trwave");

      addChildren("body", "blacklist");
      addAttrs("blacklist", "address", "contacts");

      addChildren("body", "invitation");
      addAttrs("invitation", "remaining", "title", "invitedString");
      addChildren("invitation", "invited");
      addAttrs("invited", "address");

      addAttrWithValues("eqn", "format", "tex");
      containsBlipText("eqn");

      addChildren("body", "settings");
      addAttrs("settings", "name");
      addChildren("settings", "bool-setting", "radio-setting", "text-setting", "listbox-setting");
      addAttrs("bool-setting", "id", "live-value", "saved-value");
      addAttrs("radio-setting", "id", "live-value", "saved-value");
      addAttrs("listbox-setting", "id", "live-value", "saved-value");
      addAttrs("text-setting", "id", "saved-value");
      oneLiner("text-setting");

      addChildren("body", "html");
      addChildren("html", "data");
      containsBlipText("data");

      addChildren("body", "experimental");
      addAttrs("experimental", "url");
      addChildren("experimental", "namevaluepair", "part");
      addAttrs("part", "id");
      lineContainer("part");
      containsFormElements("part");
      addAttrs("namevaluepair", "name");
      addAttrs("namevaluepair", "value");

      addChildren("body", "translation");
      addChildren("translation", "stanza");
      lineContainer("stanza");
      addAttrs("stanza", "lang", "users");

      addChildren("body", "extension_installer");
      // Can it contain form elements?
      // TODO(user): Remove img when I know it's safe.
      addAttrs("extension_installer", "manifest", "img", "installed");

      addChildren("body", "ext-settings");
      addAttrs("ext-settings", "manifest", "enabled");

      addChildren("body", "gadget-settings");
      addAttrs("gadget-settings", "url", "prefs");

      // NOTE: For now, schema constraints for height and width implemented
      // explicitly
      addAttrs("img", "alt", "height", "width", "src");

      addChildren("body", "quote");
      lineContainer("quote");
    }

    private void lineContainer(String element) {
      addChildren(element, "line", "image", "gadget", "eqn",
          "experimental", "mediasearch", "img", "reply", "profile");
      containsBlipText(element);
      addRequiredInitial(element, Collections.singletonList("line"));
    }

    private void oneLiner(String element) {
      containsBlipText(element);
      // Possibly allow other some elements, TBD
    }

    private void containsFormElements(String element) {
      addChildren(element, "button", "check", "input", "label", "password",
          "radiogroup", "radio", "textarea");
    }

    @Override
    public boolean permitsAttribute(String tag, String attr, String value) {
      // Some special cases
      if ("line".equals(tag) && "i".equals(attr)) {
        return SchemaUtils.isPositiveInteger(value);
      }

      if ("img".equals(tag) && ("width".equals(attr) || "height".equals(attr))) {
        return SchemaUtils.isValidInteger(value, 0);
      }

      if (Blips.LAST_MODIFICATION_TIME_TAGNAME.equals(tag)
          && "t".equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      }

      if ("invitation".equals(tag) && "remaining".equals(attr)) {
        return SchemaUtils.isNonNegativeInteger(value);
      }

      return super.permitsAttribute(tag, attr, value);
    }
  }  
  
}
