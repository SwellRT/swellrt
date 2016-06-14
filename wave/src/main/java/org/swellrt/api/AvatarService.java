package org.swellrt.api;

import cc.kune.initials.AvatarComposite;
import cc.kune.initials.AvatarComposite.Builder;
import cc.kune.initials.InitialLabel;
import cc.kune.initials.InitialsResources;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IsWidget;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class AvatarService {

  private static AvatarService INSTANCE = null;

  public static AvatarService getInstance() {

    if (INSTANCE == null) {
      // Ensure CSS be loaded
      InitialsResources.INS.css().ensureInjected();
      INSTANCE = new AvatarService();
    }
    return INSTANCE;
  }

  private IsWidget createNameLabel(final String name) {

    String upperCaseName = name.toLowerCase();
    upperCaseName =
        upperCaseName.substring(0, 1).toUpperCase()
            + upperCaseName.substring(1, upperCaseName.length());

    final InitialLabel l = new InitialLabel(name);
    l.getElement().getStyle().setFloat(Float.LEFT);
    return l;
  }


  private IsWidget createAvatarWidget(AvatarParameter parameter) {

    IsWidget widget = null;

    if (parameter.getElement() != null) {

      widget = HTMLPanel.wrap(parameter.getElement());

    } else if (parameter.getPicture() != null) {

      widget = new Image(parameter.getPicture());

    } else {

      String name = parameter.getName();
      if (name == null || name.isEmpty()) name = "?";

      widget = createNameLabel(name);

    }

    return widget;
  }



    /**
   * Generate a set of avatars as HTML elements. There will be created
   * options.numberOfAvatars elements, being the last avatar a multiple one if
   * there are remaining parameters.
   *
   * To generate just a single multiple avatar use options.numberOfAvatars equals to 1.
   *
   * @param parameters parameters to generate each avatar
   * @param options general options
   * @return array of DOM elements
   */
  public JsArray<Element> getAvatar(JsArray<AvatarParameter> parameters, AvatarOptions options) {

    Builder builder = new AvatarComposite.Builder(options.getSize(), options.getPadding());

    List<AvatarComposite> avatars = new ArrayList<AvatarComposite>();

    // single avatars
    int index = 0;
    while (index < options.getNumberOfAvatars() - 1) {
      avatars.add(builder.build(createAvatarWidget(parameters.get(index))));
      index++;
    }

    // multiple avatar
    LinkedList<IsWidget> widgets = new LinkedList<IsWidget>();

    for (int i = index; i < index + 4 && i < parameters.length(); i++) {
      widgets.add(createAvatarWidget(parameters.get(i)));
    }

    avatars.add(builder.build(widgets));

    // Adapt to JS array
    @SuppressWarnings("unchecked")
    JsArray<Element> elements = (JsArray<Element>) SwellRTUtils.createTypedJsArray();

    for (AvatarComposite av : avatars) {
      if (options.getCssClass() != null && !options.getCssClass().isEmpty())
        av.getElement().addClassName(options.getCssClass());
      elements.push(av.getElement());
    }

    return elements;
  }

}
