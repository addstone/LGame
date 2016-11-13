package loon.component;

import loon.LTexture;
import loon.canvas.LColor;
import loon.font.Font.Style;
import loon.font.IFont;
import loon.font.LFont;
import loon.opengl.GLEx;
import loon.opengl.LSTRDictionary;

/**
 * 该类用以创建单独的标签组件(LLables为成批渲染文字，此类为单独渲染，效率上较慢)
 * PS:具体位置可用setOffsetLeft和setOffsetTop进一步微调
 * 
 * Example1:
 * 
 * LLabel label =
 * LLabel.make(LabelAlignment.CENTER,"ABC",0,0,200,100,LColor.red);
 * 
 * Example2:
 * 
 * LLabel label = LLabel.make("ABC",99,99,LColor.red);
 * 
 */
public class LLabel extends LComponent {

	public enum LabelAlignment {
		LEFT, CENTER, RIGHT
	}

	private LColor color;

	private IFont font;

	private float backalpha = 1f, offsetLeft = 0, offsetTop = 0;

	private LabelAlignment labelAlignment;

	private LTexture background;

	private String text;

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, int size, LTexture tex, LColor c) {
		IFont font = LFont.getFont(size);
		return new LLabel(alignment, font, c, tex, mes, x, y, 0, 0);
	}

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, int width, int height, String fontname, int size) {
		return new LLabel(alignment, LFont.getFont(fontname, size),
				LColor.white, null, mes, x, y, width, height);
	}

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, int width, int height, String fontname, int size, Style style) {
		return new LLabel(alignment, LFont.getFont(fontname, style, size),
				LColor.white, null, mes, x, y, width, height);
	}

	public static LLabel make(String mes, String fontname, int size, Style style) {
		return new LLabel(LabelAlignment.CENTER, LFont.getFont(fontname, style,
				size), LColor.white, null, mes, 0, 0, 0, 0);
	}

	public static LLabel make(String mes, String fontname, int size) {
		return new LLabel(LabelAlignment.CENTER, LFont.getFont(fontname, size),
				LColor.white, null, mes, 0, 0, 0, 0);
	}

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, IFont font) {
		return new LLabel(alignment, font, LColor.white, mes, x, y);
	}

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, LColor color) {
		return new LLabel(alignment, LFont.getDefaultFont(), color, null, mes,
				x, y, 0, 0);
	}

	public static LLabel make(LabelAlignment alignment, LTexture tex,
			String mes, int x, int y, LColor color) {
		return new LLabel(alignment, LFont.getDefaultFont(), color, tex, mes,
				x, y, 0, 0);
	}

	public static LLabel make(LabelAlignment alignment, LTexture tex,
			String mes, int x, int y, int width, int height, LColor color) {
		return new LLabel(alignment, LFont.getDefaultFont(), color, tex, mes,
				x, y, width, height);
	}

	public static LLabel make(LabelAlignment alignment, String mes, int x,
			int y, int width, int height, LColor color) {
		return new LLabel(alignment, LFont.getDefaultFont(), color, null, mes,
				x, y, width, height);
	}

	public static LLabel make(LabelAlignment alignment, int size, String mes,
			int x, int y, LColor color) {
		return new LLabel(alignment, LFont.getFont(size), color, null, mes, x,
				y, 0, 0);
	}

	public static LLabel make(int size, String mes, int x, int y, LColor color) {
		return new LLabel(LabelAlignment.CENTER, LFont.getFont(size), color,
				null, mes, x, y, 0, 0);
	}

	public static LLabel make(int size, String mes, int x, int y) {
		return new LLabel(LabelAlignment.CENTER, LFont.getFont(size),
				LColor.white, null, mes, x, y, 0, 0);
	}

	public static LLabel make(String mes, int x, int y, LColor color) {
		return new LLabel(LabelAlignment.CENTER, LFont.getDefaultFont(), color,
				null, mes, x, y, 0, 0);
	}

	public static LLabel make(String mes, int x, int y) {
		return make(mes, x, y, LColor.white);
	}

	public LLabel(LabelAlignment alignment, int size, LColor c, String mes,
			int x, int y) {
		this(alignment, LFont.getFont(size), c, mes, x, y);
	}

	public LLabel(LabelAlignment alignment, IFont font, LColor c, String mes,
			int x, int y) {
		this(alignment, font, c, null, mes, x, y, font.stringWidth(mes), font
				.getHeight());
	}

	public LLabel(LabelAlignment alignment, IFont font, LColor c, LTexture bg,
			String mes, int x, int y, int width, int height) {
		super(x, y, width, height);
		if (font != null && font instanceof LFont) {
			LSTRDictionary.bind((LFont) font, mes);
		}
		this.labelAlignment = alignment;
		this.background = bg;
		this.color = c;
		this.font = font;
		this.text = mes;
		if (bg != null && (width == 0 || height == 0)) {
			setWidth(bg.getWidth());
			setHeight(bg.getHeight());
		} else if (width == 0) {
			setWidth(font.stringWidth(text) + 3);
			setHeight(font.getSize() / 2);
		}
	}

	@Override
	public void createUI(GLEx g, int x, int y, LComponent component,
			LTexture[] buttonImage) {
		draw(g, x, y);
	}

	public void draw(GLEx g, int x, int y) {
		LColor oldColor = g.getColor();
		float oldAlpha = g.getAlpha();
		if (backalpha != 1f) {
			g.setAlpha(backalpha);
		}
		if (background != null) {
			g.draw(background, x, y, getWidth(), getHeight());
		}
		if (backalpha != 1f) {
			g.setAlpha(oldAlpha);
		}
		if (text != null && text.length() > 0) {
			g.setColor(color);
			switch (labelAlignment) {
			case CENTER:
				font.drawString(g, text,
						x + (getWidth() / 2 - font.stringWidth(text) / 2)
								+ offsetLeft,
						y + (getHeight() / 2 - font.getHeight() / 2)
								+ offsetTop);
				break;
			case LEFT:
				font.drawString(g, text, x + offsetLeft, y
						+ (getHeight() / 2 - font.getHeight() / 2) + offsetTop);
				break;
			case RIGHT:
				font.drawString(g, text,
						x + getWidth() - font.stringWidth(text) + offsetLeft, y
								+ (getHeight() / 2 - font.getHeight() / 2)
								+ offsetTop);
				break;
			default:
				break;
			}
			g.setColor(oldColor);
		}
	}

	public LColor getColor() {
		return color;
	}

	public void setColor(LColor color) {
		this.color = color;
	}

	public IFont getFont() {
		return font;
	}

	public float getBackAlpha() {
		return backalpha;
	}

	public void setBackAlpha(float backalpha) {
		this.backalpha = backalpha;
	}

	public LabelAlignment getLabelAlignment() {
		return labelAlignment;
	}

	public String getText() {
		return text;
	}

	public float getOffsetLeft() {
		return offsetLeft;
	}

	public void setOffsetLeft(float offsetLeft) {
		this.offsetLeft = offsetLeft;
	}

	public float getOffsetTop() {
		return offsetTop;
	}

	public void setOffsetTop(float offsetTop) {
		this.offsetTop = offsetTop;
	}

	@Override
	public String getUIName() {
		return "Label";
	}

}
