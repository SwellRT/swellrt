package org.waveprotocol.wave.client.common.util;

import org.waveprotocol.wave.model.util.StringMap;

/**
 * Material Design palette
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class RgbColorPalette {
  
  public static RgbColor White = new RgbColor(255, 255, 255);
  public static RgbColor Black = new RgbColor(255, 255, 255);

  
  public static StringMap<RgbColor> Red= JsoStringMap.create();

  static {

    Red.put("50",new RgbColor("ffebee"));
    Red.put("100",new RgbColor("ffcdd2"));
    Red.put("200",new RgbColor("ef9a9a"));
    Red.put("300",new RgbColor("e57373"));
    Red.put("400",new RgbColor("ef5350"));
    Red.put("500",new RgbColor("f44336"));
    Red.put("600",new RgbColor("e53935"));
    Red.put("700",new RgbColor("d32f2f"));
    Red.put("800",new RgbColor("c62828"));
    Red.put("900",new RgbColor("b71c1c"));
    Red.put("A100",new RgbColor("ff8a80"));
    Red.put("A200",new RgbColor("ff5252"));
    Red.put("A400",new RgbColor("ff1744"));
    Red.put("A700",new RgbColor("d50000"));
  }


  public static StringMap<RgbColor> Pink= JsoStringMap.create();

  static {

    Pink.put("50",new RgbColor("fce4ec"));
    Pink.put("100",new RgbColor("f8bbd0"));
    Pink.put("200",new RgbColor("f48fb1"));
    Pink.put("300",new RgbColor("f06292"));
    Pink.put("400",new RgbColor("ec407a"));
    Pink.put("500",new RgbColor("e91e63"));
    Pink.put("600",new RgbColor("d81b60"));
    Pink.put("700",new RgbColor("c2185b"));
    Pink.put("800",new RgbColor("ad1457"));
    Pink.put("900",new RgbColor("880e4f"));
    Pink.put("A100",new RgbColor("ff80ab"));
    Pink.put("A200",new RgbColor("ff4081"));
    Pink.put("A400",new RgbColor("f50057"));
    Pink.put("A700",new RgbColor("c51162"));
  }





  public static StringMap<RgbColor> Purple= JsoStringMap.create();

  static {

    Purple.put("50",new RgbColor("f3e5f5"));
    Purple.put("100",new RgbColor("e1bee7"));
    Purple.put("200",new RgbColor("ce93d8"));
    Purple.put("300",new RgbColor("ba68c8"));
    Purple.put("400",new RgbColor("ab47bc"));
    Purple.put("500",new RgbColor("9c27b0"));
    Purple.put("600",new RgbColor("8e24aa"));
    Purple.put("700",new RgbColor("7b1fa2"));
    Purple.put("800",new RgbColor("6a1b9a"));
    Purple.put("900",new RgbColor("4a148c"));
    Purple.put("A100",new RgbColor("ea80fc"));
    Purple.put("A200",new RgbColor("e040fb"));
    Purple.put("A400",new RgbColor("d500f9"));
    Purple.put("A700",new RgbColor("aa00ff"));
  }

  public static StringMap<RgbColor> Deep_Purple= JsoStringMap.create();

  static {

    Deep_Purple.put("50",new RgbColor("ede7f6"));
    Deep_Purple.put("100",new RgbColor("d1c4e9"));
    Deep_Purple.put("200",new RgbColor("b39ddb"));
    Deep_Purple.put("300",new RgbColor("9575cd"));
    Deep_Purple.put("400",new RgbColor("7e57c2"));
    Deep_Purple.put("500",new RgbColor("673ab7"));
    Deep_Purple.put("600",new RgbColor("5e35b1"));
    Deep_Purple.put("700",new RgbColor("512da8"));
    Deep_Purple.put("800",new RgbColor("4527a0"));
    Deep_Purple.put("900",new RgbColor("311b92"));
    Deep_Purple.put("A100",new RgbColor("b388ff"));
    Deep_Purple.put("A200",new RgbColor("7c4dff"));
    Deep_Purple.put("A400",new RgbColor("651fff"));
    Deep_Purple.put("A700",new RgbColor("6200ea"));
  }

  public static StringMap<RgbColor> Indigo= JsoStringMap.create();

  static {

    Indigo.put("50",new RgbColor("e8eaf6"));
    Indigo.put("100",new RgbColor("c5cae9"));
    Indigo.put("200",new RgbColor("9fa8da"));
    Indigo.put("300",new RgbColor("7986cb"));
    Indigo.put("400",new RgbColor("5c6bc0"));
    Indigo.put("500",new RgbColor("3f51b5"));
    Indigo.put("600",new RgbColor("3949ab"));
    Indigo.put("700",new RgbColor("303f9f"));
    Indigo.put("800",new RgbColor("283593"));
    Indigo.put("900",new RgbColor("1a237e"));
    Indigo.put("A100",new RgbColor("8c9eff"));
    Indigo.put("A200",new RgbColor("536dfe"));
    Indigo.put("A400",new RgbColor("3d5afe"));
    Indigo.put("A700",new RgbColor("304ffe"));
  }

  public static StringMap<RgbColor> Blue= JsoStringMap.create();

  static {

    Blue.put("50",new RgbColor("e3f2fd"));
    Blue.put("100",new RgbColor("bbdefb"));
    Blue.put("200",new RgbColor("90caf9"));
    Blue.put("300",new RgbColor("64b5f6"));
    Blue.put("400",new RgbColor("42a5f5"));
    Blue.put("500",new RgbColor("2196f3"));
    Blue.put("600",new RgbColor("1e88e5"));
    Blue.put("700",new RgbColor("1976d2"));
    Blue.put("800",new RgbColor("1565c0"));
    Blue.put("900",new RgbColor("0d47a1"));
    Blue.put("A100",new RgbColor("82b1ff"));
    Blue.put("A200",new RgbColor("448aff"));
    Blue.put("A400",new RgbColor("2979ff"));
    Blue.put("A700",new RgbColor("2962ff"));
  }

  public static StringMap<RgbColor> Light_Blue= JsoStringMap.create();

  static {

    Light_Blue.put("50",new RgbColor("e1f5fe"));
    Light_Blue.put("100",new RgbColor("b3e5fc"));
    Light_Blue.put("200",new RgbColor("81d4fa"));
    Light_Blue.put("300",new RgbColor("4fc3f7"));
    Light_Blue.put("400",new RgbColor("29b6f6"));
    Light_Blue.put("500",new RgbColor("03a9f4"));
    Light_Blue.put("600",new RgbColor("039be5"));
    Light_Blue.put("700",new RgbColor("0288d1"));
    Light_Blue.put("800",new RgbColor("0277bd"));
    Light_Blue.put("900",new RgbColor("01579b"));
    Light_Blue.put("A100",new RgbColor("80d8ff"));
    Light_Blue.put("A200",new RgbColor("40c4ff"));
    Light_Blue.put("A400",new RgbColor("00b0ff"));
    Light_Blue.put("A700",new RgbColor("0091ea"));
  }

  public static StringMap<RgbColor> Cyan= JsoStringMap.create();

  static {

    Cyan.put("50",new RgbColor("e0f7fa"));
    Cyan.put("100",new RgbColor("b2ebf2"));
    Cyan.put("200",new RgbColor("80deea"));
    Cyan.put("300",new RgbColor("4dd0e1"));
    Cyan.put("400",new RgbColor("26c6da"));
    Cyan.put("500",new RgbColor("00bcd4"));
    Cyan.put("600",new RgbColor("00acc1"));
    Cyan.put("700",new RgbColor("0097a7"));
    Cyan.put("800",new RgbColor("00838f"));
    Cyan.put("900",new RgbColor("006064"));
    Cyan.put("A100",new RgbColor("84ffff"));
    Cyan.put("A200",new RgbColor("18ffff"));
    Cyan.put("A400",new RgbColor("00e5ff"));
    Cyan.put("A700",new RgbColor("00b8d4"));
  }


  public static StringMap<RgbColor> Teal= JsoStringMap.create();

  static {

    Teal.put("50",new RgbColor("e0f2f1"));
    Teal.put("100",new RgbColor("b2dfdb"));
    Teal.put("200",new RgbColor("80cbc4"));
    Teal.put("300",new RgbColor("4db6ac"));
    Teal.put("400",new RgbColor("26a69a"));
    Teal.put("500",new RgbColor("009688"));
    Teal.put("600",new RgbColor("00897b"));
    Teal.put("700",new RgbColor("00796b"));
    Teal.put("800",new RgbColor("00695c"));
    Teal.put("900",new RgbColor("004d40"));
    Teal.put("A100",new RgbColor("a7ffeb"));
    Teal.put("A200",new RgbColor("64ffda"));
    Teal.put("A400",new RgbColor("1de9b6"));
    Teal.put("A700",new RgbColor("00bfa5"));
  }

  public static StringMap<RgbColor> Green= JsoStringMap.create();

  static {

    Green.put("50",new RgbColor("e8f5e9"));
    Green.put("100",new RgbColor("c8e6c9"));
    Green.put("200",new RgbColor("a5d6a7"));
    Green.put("300",new RgbColor("81c784"));
    Green.put("400",new RgbColor("66bb6a"));
    Green.put("500",new RgbColor("4caf50"));
    Green.put("600",new RgbColor("43a047"));
    Green.put("700",new RgbColor("388e3c"));
    Green.put("800",new RgbColor("2e7d32"));
    Green.put("900",new RgbColor("1b5e20"));
    Green.put("A100",new RgbColor("b9f6ca"));
    Green.put("A200",new RgbColor("69f0ae"));
    Green.put("A400",new RgbColor("00e676"));
    Green.put("A700",new RgbColor("00c853"));
  }







  public static StringMap<RgbColor> Light_Green= JsoStringMap.create();

  static {

    Light_Green.put("50",new RgbColor("f1f8e9"));
    Light_Green.put("100",new RgbColor("dcedc8"));
    Light_Green.put("200",new RgbColor("c5e1a5"));
    Light_Green.put("300",new RgbColor("aed581"));
    Light_Green.put("400",new RgbColor("9ccc65"));
    Light_Green.put("500",new RgbColor("8bc34a"));
    Light_Green.put("600",new RgbColor("7cb342"));
    Light_Green.put("700",new RgbColor("689f38"));
    Light_Green.put("800",new RgbColor("558b2f"));
    Light_Green.put("900",new RgbColor("33691e"));
    Light_Green.put("A100",new RgbColor("ccff90"));
    Light_Green.put("A200",new RgbColor("b2ff59"));
    Light_Green.put("A400",new RgbColor("76ff03"));
    Light_Green.put("A700",new RgbColor("64dd17"));
  }





  public static StringMap<RgbColor> Lime= JsoStringMap.create();

  static {

    Lime.put("50",new RgbColor("f9fbe7"));
    Lime.put("100",new RgbColor("f0f4c3"));
    Lime.put("200",new RgbColor("e6ee9c"));
    Lime.put("300",new RgbColor("dce775"));
    Lime.put("400",new RgbColor("d4e157"));
    Lime.put("500",new RgbColor("cddc39"));
    Lime.put("600",new RgbColor("c0ca33"));
    Lime.put("700",new RgbColor("afb42b"));
    Lime.put("800",new RgbColor("9e9d24"));
    Lime.put("900",new RgbColor("827717"));
    Lime.put("A100",new RgbColor("f4ff81"));
    Lime.put("A200",new RgbColor("eeff41"));
    Lime.put("A400",new RgbColor("c6ff00"));
    Lime.put("A700",new RgbColor("aeea00"));
  }







  public static StringMap<RgbColor> Yellow= JsoStringMap.create();

  static {
    Yellow.put("50",new RgbColor("fffde7"));
    Yellow.put("100",new RgbColor("fff9c4"));
    Yellow.put("200",new RgbColor("fff59d"));
    Yellow.put("300",new RgbColor("fff176"));
    Yellow.put("400",new RgbColor("ffee58"));
    Yellow.put("500",new RgbColor("ffeb3b"));
    Yellow.put("600",new RgbColor("fdd835"));
    Yellow.put("700",new RgbColor("fbc02d"));
    Yellow.put("800",new RgbColor("f9a825"));
    Yellow.put("900",new RgbColor("f57f17"));
    Yellow.put("A100",new RgbColor("ffff8d"));
    Yellow.put("A200",new RgbColor("ffff00"));
    Yellow.put("A400",new RgbColor("ffea00"));
    Yellow.put("A700",new RgbColor("ffd600"));
  }

  public static StringMap<RgbColor> Amber= JsoStringMap.create();

  static {
    Amber.put("50",new RgbColor("fff8e1"));
    Amber.put("100",new RgbColor("ffecb3"));
    Amber.put("200",new RgbColor("ffe082"));
    Amber.put("300",new RgbColor("ffd54f"));
    Amber.put("400",new RgbColor("ffca28"));
    Amber.put("500",new RgbColor("ffc107"));
    Amber.put("600",new RgbColor("ffb300"));
    Amber.put("700",new RgbColor("ffa000"));
    Amber.put("800",new RgbColor("ff8f00"));
    Amber.put("900",new RgbColor("ff6f00"));
    Amber.put("A100",new RgbColor("ffe57f"));
    Amber.put("A200",new RgbColor("ffd740"));
    Amber.put("A400",new RgbColor("ffc400"));
    Amber.put("A700",new RgbColor("ffab00"));
  }

  public static StringMap<RgbColor> Orange= JsoStringMap.create();

  static {
    Orange.put("50",new RgbColor("fff3e0"));
    Orange.put("100",new RgbColor("ffe0b2"));
    Orange.put("200",new RgbColor("ffcc80"));
    Orange.put("300",new RgbColor("ffb74d"));
    Orange.put("400",new RgbColor("ffa726"));
    Orange.put("500",new RgbColor("ff9800"));
    Orange.put("600",new RgbColor("fb8c00"));
    Orange.put("700",new RgbColor("f57c00"));
    Orange.put("800",new RgbColor("ef6c00"));
    Orange.put("900",new RgbColor("e65100"));
    Orange.put("A100",new RgbColor("ffd180"));
    Orange.put("A200",new RgbColor("ffab40"));
    Orange.put("A400",new RgbColor("ff9100"));
    Orange.put("A700",new RgbColor("ff6d00"));
  }

  public static StringMap<RgbColor> Deep_Orange= JsoStringMap.create();

  static {

    Deep_Orange.put("50",new RgbColor("fbe9e7"));
    Deep_Orange.put("100",new RgbColor("ffccbc"));
    Deep_Orange.put("200",new RgbColor("ffab91"));
    Deep_Orange.put("300",new RgbColor("ff8a65"));
    Deep_Orange.put("400",new RgbColor("ff7043"));
    Deep_Orange.put("500",new RgbColor("ff5722"));
    Deep_Orange.put("600",new RgbColor("f4511e"));
    Deep_Orange.put("700",new RgbColor("e64a19"));
    Deep_Orange.put("800",new RgbColor("d84315"));
    Deep_Orange.put("900",new RgbColor("bf360c"));
    Deep_Orange.put("A100",new RgbColor("ff9e80"));
    Deep_Orange.put("A200",new RgbColor("ff6e40"));
    Deep_Orange.put("A400",new RgbColor("ff3d00"));
    Deep_Orange.put("A700",new RgbColor("dd2c00"));
  }


  public static StringMap<RgbColor> Brown= JsoStringMap.create();

  static {

    Brown.put("50",new RgbColor("efebe9"));
    Brown.put("100",new RgbColor("d7ccc8"));
    Brown.put("200",new RgbColor("bcaaa4"));
    Brown.put("300",new RgbColor("a1887f"));
    Brown.put("400",new RgbColor("8d6e63"));
    Brown.put("500",new RgbColor("795548"));
    Brown.put("600",new RgbColor("6d4c41"));
    Brown.put("700",new RgbColor("5d4037"));
    Brown.put("800",new RgbColor("4e342e"));
    Brown.put("900",new RgbColor("3e2723"));
  }







  public static StringMap<RgbColor> Grey= JsoStringMap.create();

  static {

    Grey.put("50",new RgbColor("fafafa"));
    Grey.put("100",new RgbColor("f5f5f5"));
    Grey.put("200",new RgbColor("eeeeee"));
    Grey.put("300",new RgbColor("e0e0e0"));
    Grey.put("400",new RgbColor("bdbdbd"));
    Grey.put("500",new RgbColor("9e9e9e"));
    Grey.put("600",new RgbColor("757575"));
    Grey.put("700",new RgbColor("616161"));
    Grey.put("800",new RgbColor("424242"));
    Grey.put("900",new RgbColor("212121"));
  }





  public static StringMap<RgbColor> Blue_Grey= JsoStringMap.create();

  static {

    Blue_Grey.put("50",new RgbColor("eceff1"));
    Blue_Grey.put("100",new RgbColor("cfd8dc"));
    Blue_Grey.put("200",new RgbColor("b0bec5"));
    Blue_Grey.put("300",new RgbColor("90a4ae"));
    Blue_Grey.put("400",new RgbColor("78909c"));
    Blue_Grey.put("500",new RgbColor("607d8b"));
    Blue_Grey.put("600",new RgbColor("546e7a"));
    Blue_Grey.put("700",new RgbColor("455a64"));
    Blue_Grey.put("800",new RgbColor("37474f"));
    Blue_Grey.put("900",new RgbColor("263238"));
  }


  @SuppressWarnings({ "rawtypes", "unchecked" })
  public static final StringMap<RgbColor>[] PALETTE = new StringMap[]  {
      Red, Amber, Blue, Blue_Grey, Brown, Cyan, Deep_Orange, Deep_Purple, Green, Indigo, Light_Blue, Light_Green, Lime, Orange, Pink, Purple, Teal, Yellow
  };  



}
