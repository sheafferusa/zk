/* Fusionchart.java

	Purpose:
		
	Description:
		
	History:
		Jan 8, 2011 2:13:47 AM, Created by jimmyshiau

Copyright (C) 2011 Potix Corporation. All Rights Reserved.

{{IS_RIGHT
	This program is distributed under LGPL Version 3.0 in the hope that
	it will be useful, but WITHOUT ANY WARRANTY.
}}IS_RIGHT
 */
package org.zkoss.zul;

import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

import org.zkoss.lang.*;
import org.zkoss.util.TimeZones;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuRequests;
import org.zkoss.zk.au.out.AuSetAttribute;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.MouseEvent;
import org.zkoss.zk.ui.sys.ContentRenderer;
import org.zkoss.zul.FusionchartCategoryModel.*;
import org.zkoss.zul.FusionchartPieModel.FusionchartPieData;
import org.zkoss.zul.event.ChartDataEvent;
import org.zkoss.zul.event.ChartDataListener;

/**
 * The Fusionchart component. Developers set proper chart type, data model,
 * and the threeD (3D) attribute to draw proper chart.
 *
 * <p>The model and type must
 * match to each other; or the result is unpredictable. The 3D chart is not supported
 * on all chart type.
 *
 * <table>
 *   <tr><th>type</th><th>model</th><th>3D</th></tr>
 *   <tr><td>area</td><td>{@link CategoryModel} or {@link XYModel}</td><td>No</td></tr>
 *   <tr><td>bar</td><td>{@link CategoryModel}</td><td>Yes</td></tr>
 *   <tr><td>gantt</td><td>{@link GanttModel}</td><td>No</td></tr>
 *   <tr><td>line</td><td>{@link CategoryModel} or {@link XYModel}</td><td>Yes</td></tr>
 *   <tr><td>pie</td><td>{@link PieModel}</td><td>Yes</td></tr>
 *   <tr><td>stacked_bar</td><td>{@link CategoryModel}</td><td>Yes</td></tr>
 *   <tr><td>stacked_area</td><td>{@link CategoryModel} or {@link XYModel}</td><td>No</td></tr>
 * </table>
 *
 * @see ChartModel
 * @author jimmyshiau
 */
public class Fusionchart extends HtmlBasedComponent {

	private static final long serialVersionUID = 20110107232220L;

	// chart type
	public static final String COMBINATION = "combination";

	public static final Paint[] DEFAULT_PAINT_SEQUENCE = ChartColor.createDefaultPaintArray();
	private static final Map DEFAULT_MODEL = new HashMap();
	static {
		DEFAULT_MODEL.put(Chart.PIE, "org.zkoss.zul.FusionchartPieModel");
		DEFAULT_MODEL.put(Chart.BAR, "org.zkoss.zul.FusionchartCategoryModel");
		DEFAULT_MODEL.put(Chart.LINE, "org.zkoss.zul.FusionchartXYModel");
		DEFAULT_MODEL.put(Chart.AREA, "org.zkoss.zul.FusionchartCategoryModel");
		DEFAULT_MODEL.put(Chart.STACKED_BAR,
				"org.zkoss.zul.FusionchartCategoryModel");
		DEFAULT_MODEL.put(Chart.STACKED_AREA,
				"org.zkoss.zul.FusionchartCategoryModel");
		DEFAULT_MODEL.put(Fusionchart.COMBINATION, "org.zkoss.zul.FusionchartCategoryModel");
		DEFAULT_MODEL.put(Chart.GANTT, "org.zkoss.zul.FusionchartGanttModel");
	}

	//control variable
	private int _paintIndex = 0;
	private boolean _smartDrawChart; // whether post the smartDraw event already?
	private EventListener _smartDrawChartListener; // the smartDrawListner
	private ChartDataListener _dataListener;

	private String _type = Chart.PIE; // chart type (pie, ring, bar, line, xy, etc)
	private boolean _threeD; // whether a 3D chart
	
	//chart related attributes
	private String _title; // chart title
	private String _xAxis;
	private String _yAxis;
	private boolean _showLegend = true; // wether show legend
	private String _orient = "vertical"; // orient
	private String _paneColor; // pane's color
    private int[] _paneRGB = new int[] {0xEE,0xEE,0xEE}; //pane red, green, blue (0 ~ 255, 0 ~ 255, 0 ~ 255)
    private int _paneAlpha = 255; //pane alpha transparency (0 ~ 255, default to 255)
		
	
    private boolean _useChartFgAlpha = true;
	private int _fgAlpha = 255; //foreground alpha transparency (0 ~ 255, default to 255)
	private String _bgColor;
	private int[] _bgRGB = new int[] {0xFF,0xFF,0xFF}; //background red, green, blue (0 ~ 255, 0 ~ 255, 0 ~ 255)
	private int _bgAlpha = 255; //background alpha transparency (0 ~ 255, default to 255)

	//Time Series Chart related attributes
	private TimeZone _tzone;
	private String _period;
	private String _dateFormat;
	
	//chart data model
	private ChartModel _model; //chart data model
	
	//chart Font
	private Font _categoryFont =  new Font("Tahoma", Font.PLAIN, 12);
	private Font _baseFont =  new Font("Tahoma", Font.PLAIN, 12);
	private Font _outCnvBaseFont =  new Font("Tahoma", Font.PLAIN, 12);
	
	
	//FusionChart related attributes
	private int _decimalPrecision = 0;

	public Fusionchart() {
		init();
		setWidth("500");
		setHeight("250");
	}

	private ChartModel createDefaultModel() {
		final String klass = (String) DEFAULT_MODEL.get(getType());
		if (klass != null) {
			try {
				return (ChartModel) Classes.newInstanceByThread(klass);
			} catch (Exception e) {
				throw UiException.Aide.wrap(e);
			}
		} else {
			throw new UiException("unknown chart type: "+getType());
		}
	}
	
	private void init() {
		if (_smartDrawChartListener == null) {
			_smartDrawChartListener = new SmartDrawListener();
			addEventListener("onSmartDrawChart", _smartDrawChartListener);
		}
	}
	
	/**
	 * Set the chart's type (Chart.PIE, Chart.BAR, Chart.LINE, etc.).
	 *
	 * <p>Default: pie.
	 *
	 */
	public void setType(String type) {
		if (!Objects.equals(_type, type)) {
			_type = type;
			smartUpdate("type", type);
			smartDrawChart();
		}
	}
	
	/**
	 * Get the chart's type.
	 */
	public String getType() {
		return _type;
	}

	/**
	 * Set true to show three dimensional graph (If a type of chart got no 3d peer, this is ignored).
	 */
	public void setThreeD(boolean threeD) {
		if (_threeD != threeD) {
			_threeD = threeD;
			smartUpdate("threeD", threeD);
			smartDrawChart();
		}
	}
	
	/**
	 * Whether a 3d chart.
	 */
	public boolean isThreeD() {
		return _threeD;
	}

	/**
	 * Set the chart's title.
	 * @param title the chart's title.
	 *
	 */
	public void setTitle(String title) {
		if (!Objects.equals(_title, title)) {
			_title = title;
			smartDrawChart();
		}
	}
	
	/**
	 * Get the chart's title.
	 */
	public String getTitle() {
		return _title;
	}
	
	/**
	 * Set the label in xAxis.
	 * @param label label in xAxis.
	 */
	public void setXAxis(String label) {
		if (!Objects.equals(_xAxis, label)) {
			_xAxis = label;
			smartDrawChart();
		}
	}
	
	/**
	 * Get the label in xAxis.
	 */
	public String getXAxis() {
		return _xAxis;
	}
	
	/**
	 * Set the label in yAxis.
	 * @param label label in yAxis.
	 */
	public void setYAxis(String label) {
		if (!Objects.equals(_yAxis, label)) {
			_yAxis = label;
			smartDrawChart();
		}
	}
	
	/**
	 * Get the label in yAxis.
	 */
	public String getYAxis() {
		return _yAxis;
	}
	
	/**
	 * whether show the chart's legend.
	 * @param showLegend true if want to show the legend (default to true).
	 */
	public void setShowLegend(boolean showLegend) {
		if (!_showLegend == showLegend) {
			_showLegend = showLegend;
			smartDrawChart();
		}
	}
	
	/**
	 * Check whether show the legend of the chart.
	 */
	public boolean isShowLegend() {
		return _showLegend;
	}
	
	/**
	 * Set the pane alpha (transparency, 0 ~ 255).
	 * @param alpha the transparency of pane color (0 ~ 255, default to 255 opaque).
	 */
	public void setPaneAlpha(int alpha) {
		if (alpha != _paneAlpha) {
			if (alpha > 255 || alpha < 0)
				alpha = 255;
			_paneAlpha = alpha;
			smartDrawChart();
		}
	}
	
	/**
	 * Get the pane alpha (transparency, 0 ~ 255, opacue).
	 */
	public int getPaneAlpha() {
		return _paneAlpha;
	}

	/**
	 * Set the pane color of the chart.
	 * @param color in #RRGGBB format (hexdecimal).
	 */
	public void setPaneColor(String color) {
		if (!Objects.equals(color, _paneColor)) {
			_paneColor = color;
			if (_paneColor == null) {
				_paneRGB = null;
			} else {
				_paneRGB = new int[3];
				Chart.decode(_paneColor, _paneRGB);
			}
			smartDrawChart();
		}
	}
	
	/**
	 * Get the pane color of the chart (in string as #RRGGBB).
	 * null means default.
	 */
	public String getPaneColor() {
		return _paneColor;
	}
	
	/**
	 * Get the pane color in int array (0: red, 1: green, 2:blue).
	 * null means default.
	 */
	public int[] getPaneRGB() {
		return _paneRGB;
	}

	/**
	 * Returns whether to set the foreground alpha from char,
	 * otherwise to set the foreground alpha from char model.
	 * @return boolean
	 */
	public boolean isUseChartFgAlpha() {
		return _useChartFgAlpha;
	}

	/**
	 * Sets whether to set the foreground alpha from char,
	 * otherwise to set the foreground alpha from char model.
	 * @param useChartFgAlpha
	 */
	public void setUseChartFgAlpha(boolean useChartFgAlpha) {
		if (useChartFgAlpha != _useChartFgAlpha) {
			this._useChartFgAlpha = useChartFgAlpha;
			smartDrawChart();
		}
	}
	
	/**
	 * Set the foreground alpha (transparency, 0 ~ 255).
	 * @param alpha the transparency of foreground color (0 ~ 255, default to 255 opaque).
	 */
	public void setFgAlpha(int alpha) {
		if (alpha != _fgAlpha) {
			if (alpha > 255 || alpha < 0) {
				alpha = 255;
			}
			_fgAlpha = alpha;
			smartDrawChart();
		}
	}

	/**
	 * Get the foreground alpha (transparency, 0 ~ 255, opacue).
	 */
	public int getFgAlpha() {
		return _fgAlpha;
	}

	/**
	 * Set the background alpha (transparency, 0 ~ 255).
	 * @param alpha the transparency of background color (0 ~ 255, default to 255 opaque).
	 */
	public void setBgAlpha(int alpha) {
		if (alpha != _bgAlpha) {
			if (alpha > 255 || alpha < 0) {
				alpha = 255;
			}
			_bgAlpha = alpha;
			smartDrawChart();
		}
	}
	
	/**
	 * Get the background alpha (transparency, 0 ~ 255, opacue).
	 */
	public int getBgAlpha() {
		return _bgAlpha;
	}

	/**
	 * Set the background color of the chart.
	 * @param color in #RRGGBB format (hexdecimal).
	 */
	public void setBgColor(String color) {
		if (!Objects.equals(color, _bgColor)) {
			_bgColor = color;
			if (_bgColor == null) {
				_bgRGB = null;
			} else {
				_bgRGB = new int[3];
				Chart.decode(_bgColor, _bgRGB);
			}
			smartDrawChart();
		}
	}
	
	/**
	 * Get the background color of the chart (in string as #RRGGBB).
	 * null means default.
	 */
	public String getBgColor() {
		return _bgColor;
	}
	
	/**
	 * Get the background color in int array (0: red, 1: green, 2:blue).
	 * null means default.
	 */
	public int[] getBgRGB() {
		return _bgRGB;
	}
	
	/**
	 * Set the chart orientation.
	 * @param orient vertical or horizontal (default to vertical)
	 */
	public void setOrient(String orient) {
		if (!Objects.equals(orient, _orient)) {
			_orient = orient;
			smartUpdate("orient", orient);
			smartDrawChart();
		}
	}
	
	/**
	 * Get the chart orientation (vertical or horizontal)
	 */
	public String getOrient() {
		return _orient;
	}
	
	/** Returns the time zone that this Time Series Chart belongs to, or null if
	 * the default time zone is used.
	 * <p>The default time zone is determined by {@link org.zkoss.util.TimeZones#getCurrent}.
	 */
	public TimeZone getTimeZone() {
		return _tzone;
	}
	/** Sets the time zone that this Time Series Chart belongs to, or null if
	 * the default time zone is used.
	 * <p>The default time zone is determined by {@link org.zkoss.util.TimeZones#getCurrent}.
	 */
	public void setTimeZone(TimeZone tzone) {
		if (Objects.equals(tzone, _tzone)) {
			return;
		}
		_tzone = tzone;
		smartDrawChart();
	}

	/** Returns the period used in Time Series Chart. The value can be
	 * "millisecond", "second", "minute", "hour", "day", "week", "month", "quarter", and "year".
	 * default is "millisecond" if not specified.
	 */
	public String getPeriod() {
		return _period;
	}
	
	/** Sets the period used in Time Series Chart. The value can be
	 * "millisecond", "second", "minute", "hour", "day", "week", "month", "quarter", and "year".
	 */
	public void setPeriod(String period) {
		if (Objects.equals(period, _period)) {
			return;
		}
		_period = period;
		smartDrawChart();
	}
	
	/**
	 * Returns the date format used by date related Chart.
	 * @return the date format used by date related Chart..
	 */
	public String getDateFormat() {
		return _dateFormat;
	}
	
	/**
	 * Sets the date format used by date related Chart.
	 * @param format
	 */
	public void setDateFormat(String format) {
		if (Objects.equals(format, _dateFormat)) {
			return;
		}
		_dateFormat = format;
		smartDrawChart();
	}
	
	/**
	 * Returns the font of the category names.
	 * @return the category font
	 */
	public Font getCategoryFont() {
		return _categoryFont;
	}

	/**
	 * Sets the font of the category names.
	 * @param font the category font of this chart 
	 */
	public void setCategoryFont(Font font) {
		if (!Objects.equals(font, _categoryFont)) {
			_categoryFont = font;
			smartDrawChart();
		}
	}
	
	/**
	 * Returns the base font of the chart which lies on the canvas i.e., 
	 * all the values and the names in the chart which lie on the canvas 
	 * will be displayed using the font provided here.
	 * @return Font
	 */
	public Font getBaseFont() {
		return _baseFont;
	}

	/**
	 * Sets the base font of the chart which lies on the canvas i.e., 
	 * all the values and the names in the chart which lie on the canvas 
	 * will be displayed using the font provided here.
	 * @param font
	 */
	public void setBaseFont(Font font) {
		if (!Objects.equals(_baseFont, font)) {
			_baseFont = font;
			smartDrawChart();
		}
	}
	
	/**
	 * Returns the base font of the chart which lies on the canvas i.e., 
	 * all the values and the names in the chart which lie on the canvas 
	 * will be displayed using the font provided here.
	 * @return Font
	 */
	public Font getOutCnvBaseFont() {
		return _outCnvBaseFont;
	}

	/**
	 * Sets the base font of the chart font which lies outside the canvas 
	 * i.e., all the values and the names in the chart which lie outside 
	 * the canvas will be displayed using the font name provided here. 
	 * @param font
	 */
	public void setOutCnvBaseFont(Font font) {
		if (!Objects.equals(_outCnvBaseFont, font)) {
			_outCnvBaseFont = font;
			smartDrawChart();
		}
	}
	
	/**
	 * Returns which all numbers on the chart would be rounded to.
	 * <p>
	 * Default: 100.
	 * @return int
	 */
	public int getDecimalPrecision() {
		return _decimalPrecision;
	}

	/**
	 * Sets which all numbers on the chart would be rounded to.
	 * <p>
	 * Default: 100.
	 * @param decimalPrecision
	 */
	public void setDecimalPrecision(int decimalPrecision) {
		if (_decimalPrecision != decimalPrecision) {
			this._decimalPrecision = decimalPrecision;
			smartDrawChart();
		}
	}
	
	/** Returns the chart model associated with this chart, or null
	 * if this chart is not associated with any chart data model.
	 */
	public ChartModel getModel() {
		return _model;
	}

	/** Sets the chart model associated with this chart.
	 * If a non-null model is assigned, no matter whether it is the same as
	 * the previous, it will always cause re-render.
	 *
	 * @param model the chart model to associate, or null to dis-associate
	 * any previous model.
	 * @exception UiException if failed to initialize with the model
	 */
	public void setModel(ChartModel model) {
		if (_model != model) {
			if (_model != null) {
				_model.removeChartDataListener(_dataListener);
			}
			_model = model;
			initDataListener();
		}
		
		//Always redraw
		smartDrawChart();
	}
	
	/** Sets the model by use of a class name.
	 * It creates an instance automatically.
	 */
	public void setModel(String clsnm)
	throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
	InstantiationException, java.lang.reflect.InvocationTargetException {
		if (clsnm != null) {
			setModel((ChartModel)Classes.newInstanceByThread(clsnm));
		}
	}

	private void initDataListener() {
		if (_dataListener == null) {
			_dataListener = new MyChartDataListener();
			_model.addChartDataListener(_dataListener);
		}
	}
	
	private class MyChartDataListener implements ChartDataListener, Serializable {
		private static final long serialVersionUID = 20091008183622L;

		public void onChange(ChartDataEvent event) {
			smartDrawChart();
		}
	}

	private class SmartDrawListener implements EventListener, Serializable {
		private static final long serialVersionUID = 20091008183610L;

		public void onEvent(Event event) throws Exception {
			doSmartDraw();
		}
	}

	private void doSmartDraw() {
		if (Strings.isBlank(getType()))
			throw new UiException("chart must specify type (pie, bar, line, ...)");

		if (_model == null) {
			_model = createDefaultModel();
		}

		if (Strings.isBlank(getWidth()))
			throw new UiException("chart must specify width");
			
		if (Strings.isBlank(getHeight()))
			throw new UiException("chart must specify height");
			
		try {
			_paintIndex = 0;
			response(new AuSetAttribute(this, "fusionChartXML", getChartImpl().createChartXML()));
		} finally {
			_smartDrawChart = false;
		}
	}
	
	/**
	 * create specific type of chart drawing engine. This implementation use 
	 * JFreeChart engine.
	 */
	private ChartImpl getChartImpl() {
		ChartImpl _chartImpl;
		String type = getType();
		boolean threeD = isThreeD();
		String errMsg = "";
		String orient = getOrient();

		if (Chart.PIE.equals(type)) {
			_chartImpl = new Pie();
		} else if (Chart.BAR.equals(type) || Fusionchart.COMBINATION.equals(type)) {
			if (threeD && "horizontal".equals(orient))
				errMsg = orient + " " +type;
			_chartImpl = new Bar();
		} else if (Chart.LINE.equals(type)) {
			if (threeD)
				errMsg = type;
			_chartImpl = new Line();
		} else if (Chart.AREA.equals(type)) {
			if (threeD)
				errMsg = type;
			_chartImpl = new AreaImpl();
		} else if (Chart.STACKED_BAR.equals(type)) {
			_chartImpl = new StackedBar();
		} else if (Chart.STACKED_AREA.equals(type)) {
			if (threeD)
				errMsg = type;
			_chartImpl = new StackedArea();
		} else if (Chart.GANTT.equals(type)) {
			if (threeD)
				errMsg = type;
			_chartImpl = new Gantt();
			final TimeZone tz = getTimeZone();
			final SimpleDateFormat df = new SimpleDateFormat();
			df.setTimeZone(tz != null ? tz: TimeZones.getCurrent());
			if (getDateFormat() == null) {
				df.applyPattern("MMM d ''yy");
			} else {
				df.applyPattern(getDateFormat());
			}
		} else 
			throw new UiException("Unsupported chart type yet: "+type);
		
		
		if (!Strings.isBlank(errMsg))
			throw new UiException("Unsupported chart type yet: "+errMsg +" in threeD.");

		return _chartImpl;
	}
	
	//-- Chart specific implementation --//
	/** base chart */
	abstract private class ChartImpl {
		abstract String createChartXML();
		protected String createChart(String dataset){
			StringBuffer sb = new StringBuffer("<graph");
			
			Font baseFont = getBaseFont();
			if (baseFont != null)
				sb.append(" baseFont='").append(baseFont.getFontName())
					.append("' baseFontSize='").append(baseFont.getSize()).append("'");
			
			sb.append(" bgAlpha='").append(toFusionchartAlpha(getPaneAlpha()))
				.append("' decimalPrecision='").append(getDecimalPrecision()).append("'")
				.append(toFusionchartAttr("bgColor", toFusionchartColor(getPaneColor())))
				.append(toFusionchartAttr("caption", getTitle()));
			
			renderProperties(sb);
			
			sb.append(">").append(dataset).append("</graph>");
			return sb.toString();
		}
		protected void renderProperties(StringBuffer sb) {
		}
	}
	
	abstract private class AbstractChartImpl extends ChartImpl{
		protected void renderProperties(StringBuffer sb){
			Font outCnvBaseFont = getOutCnvBaseFont();
			if (outCnvBaseFont != null)
				sb.append(" outCnvBaseFont='").append(outCnvBaseFont.getFontName())
					.append("' outCnvBaseFontSze='").append(outCnvBaseFont.getSize()).append("'");
			
			sb.append(" canvasBgAlpha='").append(toFusionchartAlpha(getBgAlpha()))
				.append("' showLegend='").append(isShowLegend() ? '1': '0').append("'")
				.append(toFusionchartAttr("canvasBgColor", toFusionchartColor(getBgColor())))
				.append(toFusionchartAttr("xaxisname", getXAxis()))
				.append(toFusionchartAttr("yaxisname", getYAxis()));
		}
	}
	
	private class CategoryRender {
		private StringBuffer sb;
		private CategoryRender () {
			sb = new StringBuffer();
			sb.append("<categories");
			Font font = getCategoryFont();
			
			if (font != null)
				sb.append(" font='").append(font.getFontName())
					.append("' fontSize='").append(font.getSize()).append("'");
			
			sb.append(">");
		}
		
		private void renderCategory(Object category) {
			sb.append("<category name='").append(category).append("'");
			
			if (category instanceof FusionchartCategory) {
				FusionchartCategory fcategory = (FusionchartCategory) category;
				sb.append(toFusionchartAttr("hoverText", fcategory.getHoverText()))
					.append(" showName='").append(fcategory.isShowName() ? '1': '0').append("'");
			}
			sb.append("/>");
		}
		private StringBuffer finishRender() {
			return sb.append("</categories>");
		}
	}
	
	private class SeriesRender {
		private boolean _useChartFgAlpha;
		private boolean _listenedonClick;
		private StringBuffer sb;
		private String chartType;
		
		private SeriesRender (String chartType, boolean useChartFgAlpha, boolean listenedonClick) {
			this.chartType = chartType;
			this._useChartFgAlpha = useChartFgAlpha;
			this._listenedonClick = listenedonClick;
			sb = new StringBuffer();
		}
		
		protected boolean isUseChartFgAlpha() {
			return _useChartFgAlpha;
		}
		
		protected boolean isListenedonClick() {
			return _listenedonClick;
		}
		
		public void renderSeries(Comparable series) {
			sb.append("<dataset seriesName='").append(series).append("'");

			if (isUseChartFgAlpha())
				sb.append(" ").append(Chart.AREA.equals(chartType) ? 
					"areaAlpha": "alpha").append("='")
					.append(toFusionchartAlpha(getFgAlpha())).append("'");
			
			if (series instanceof FusionchartSeries)
				renderFusionchartSeries((FusionchartSeries) series);
			else  
				sb.append(" color='").append(getNextColor()).append("' showValues='0' showAreaBorder='0'");

			sb.append(">");
		}
		
		private void renderFusionchartSeries(FusionchartSeries series) {
			String color = toFusionchartColor(series.getColor());
			sb.append(" color='").append(color == null ? 
					getNextColor(): color)
				.append("' showValues='").append(series.isShowValues() ? '1' : '0').append("'");
			
			if (series instanceof FusionchartAreaSeries)
				renderFusionchartAreaSeries((FusionchartAreaSeries) series);
			else if (series instanceof FusionchartLineSeries)
				renderFusionchartLineSeries((FusionchartLineSeries) series);
			else if (!isUseChartFgAlpha()) {
				sb.append(" ").append(Chart.AREA.equals(chartType) ? 
					"areaAlpha": "alpha").append("='")
					.append(toFusionchartAlpha(series.getAlpha()))
					.append("'");
			}
		}

		private void renderFusionchartAreaSeries(FusionchartAreaSeries series) {
			if (!_useChartFgAlpha)
				sb.append(" areaAlpha='")
					.append(toFusionchartAlpha(series.getAlpha())).append("'");
			sb.append(" showAreaBorder='").append(
					series.isShowAreaBorder() ? '1' : '0').append("'")
				.append(toFusionchartAttr("areaBorderColor", 
						toFusionchartColor(series.getAreaBorderColor())))
				.append(toFusionchartAttr("areaBorderThickness", 
						series.getAreaBorderThickness()+""));
		}
		
		private void renderFusionchartLineSeries(FusionchartLineSeries series) {
			sb.append(toFusionchartAttr("lineThickness", series.getLineThickness()+""));
			AnchorProperty anchor = series.getAnchorProperty();
			if (anchor != null) {
				sb.append(" showAnchors='").append(
						anchor.isShowAnchors() ? '1' : '0')
					.append("' anchorBgAlpha='").append(toFusionchartAlpha(anchor.getBgAlpha()))
					.append("' anchorAlpha='").append(toFusionchartAlpha(anchor.getAlpha())).append("'")
					.append(toFusionchartAttr("anchorBorderThickness", anchor.getBorderThickness()+""))
					.append(toFusionchartAttr("anchorSides", anchor.getSides()+""))
					.append(toFusionchartAttr("anchorRadius", anchor.getRadius()+""))
					.append(toFusionchartAttr("anchorBorderColor", 
						toFusionchartColor(anchor.getBorderColor())))
					.append(toFusionchartAttr("anchorBgColor", 
						toFusionchartColor(anchor.getBgColor())));
			}
			
			if (series instanceof FusionchartCombinSeries)
				renderFusionchartCombinationSeries((FusionchartCombinSeries) series);
		}
		
		private void renderFusionchartCombinationSeries(FusionchartCombinSeries series) {
			sb.append(toFusionchartAttr("parentYAxis", series.getParentYAxis()))
				.append(toFusionchartAttr("numberPrefix", series.getNumberPrefix()))
				.append(toFusionchartAttr("numberSuffix", series.getNumberSuffix()));
		}

		protected void renderSeriesSet(Comparable series, Comparable category, CategoryModel model) {
			sb.append("<set value='").append(model.getValue(series, category)).append("'");
			
			int seriIndex = ((List)model.getSeries()).indexOf(series);
			int cateIndex = ((List)model.getCategories()).indexOf(category);
			
			if (isListenedonClick())
				sb.append(" link='JavaScript:zk.Widget.$(\"").append(getUuid()).append("\").clickChart(\"")
						.append(seriIndex).append("\",\"").append(cateIndex).append("\");'");
			
			if (model instanceof FusionchartCategoryModel) {
				FusionchartData data = ((FusionchartCategoryModel) model).getFusionchartData(series, category);
				
				if (data != null) {
					sb.append(toFusionchartAttr("color", toFusionchartColor(data.getColor())));
					
					if (!isListenedonClick())
						sb.append(toFusionchartAttr("link", data.getLink()));
					
					if (!isUseChartFgAlpha())
						sb.append(" alpha='").append(toFusionchartAlpha(data.getAlpha())).append("'");
				}
			}
			
			sb.append("/>");
		}
		
		public void renderSeriesEnd() {
			sb.append("</dataset>");
		}
		
		public StringBuffer getSb() {
			return sb;
		};
	}
	
	private class XYModelSeriesRender extends SeriesRender {

		XYModelSeriesRender(String chartType, boolean isUseChartFgAlpha, boolean listenedonClick) {
			super(chartType, isUseChartFgAlpha, listenedonClick);
		}
		
		protected void renderSeriesSet(Comparable series, int index, XYModel model) {
			StringBuffer sb = getSb();
			
			sb.append("<set value='").append(model.getY(series, index)).append("'");
			
			boolean isListenedonClick = isListenedonClick();
			if (isListenedonClick)
				sb.append(" link='JavaScript:zk.Widget.$(\"").append(getUuid()).append("\").clickChart(\"")
					.append(((List)model.getSeries()).indexOf(series)).append("\",\"").append(index).append("\");'");
			if (model instanceof FusionchartXYModel) {
				FusionchartData data = ((FusionchartXYModel) model).getFusionchartData(series, index);
				
				if (!isListenedonClick)
					sb.append(toFusionchartAttr("link", data.getLink()));
				
				if (!isUseChartFgAlpha())
					sb.append(" alpha='").append(toFusionchartAlpha(data.getAlpha())).append("'");
			}
			
			sb.append("/>");
		}
		
		protected void renderSeriesSet(String value) {
			getSb().append("<set value='").append(value).append("'/>");
		}
	}
		
	
	/**
	 * transfer a PieModel into Fusionchart PieDataset.
	 */
	private String PieModelToPieDataset(PieModel model) {
		StringBuffer sb = new StringBuffer();
		boolean isListenedonClick = Events.isListened(Fusionchart.this, Events.ON_CLICK, false);
		
		Collection cates = model.getCategories();
		for (final Iterator it = cates.iterator(); it.hasNext();) {
			Comparable category = (Comparable) it.next();
			
			int cateIndex = ((List)cates).indexOf(category);
			
			sb.append("<set value='").append(model.getValue(category))
				.append("' name='").append(category).append("'");
			
			if (model instanceof FusionchartPieModel) {
				FusionchartPieData data = ((FusionchartPieModel) model).getPieData(category);
				String color = toFusionchartColor(data.getColor());
				sb.append(" color='").append(color == null ? 
						getNextColor(): color)
					.append("' alpha='").append(toFusionchartAlpha(data.getAlpha()))
					.append("' isSliced='").append(data.isSliced() ? '1' : '0').append("'")
					.append(toFusionchartAttr("hoverText", data.getHoverText()));
				
				if (isListenedonClick)
					sb.append(" link='JavaScript:zk.Widget.$(\"").append(getUuid())
						.append("\").clickChart(\"\",\"").append(cateIndex).append("\");'");
				else 
					sb.append(toFusionchartAttr("link", data.getLink()));
			} else {
				sb.append(" color='").append(getNextColor()).append("'");
				if (isListenedonClick)
					sb.append(" link='JavaScript:zk.Widget.$(\"").append(getUuid())
						.append("\").clickChart(\"\",\"").append(cateIndex).append("\");'");
			}
			
			sb.append("/>");
		}
		return sb.toString();
	}

	/**
	 * transfer a CategoryModel into Fusionchart PieDataset.
	 */
	private String CategoryModelToPieDataset(CategoryModel model) {
		StringBuffer sb = new StringBuffer();
		Comparable defaultSeries = null;
		int max = 0;
		for (final Iterator it = model.getKeys().iterator(); it.hasNext();) {
			final List key = (List) it.next();
			Comparable series = (Comparable) key.get(0);
			if (defaultSeries == null) {
				defaultSeries = series;
				max = model.getCategories().size();
			}
			if (!Objects.equals(defaultSeries, series)) {
				continue;
			}
			Comparable category = (Comparable) key.get(1);
			Number value = (Number) model.getValue(series, category);
//			dataset.setValue(category, value);
			if (--max == 0) break; //no more in this series
		}
		return sb.toString();
	}
	
	/**
	 * transfer a CategoryModel into JFreeChart CategoryDataset.
	 */
	private String CategoryModelToCategoryDataset(CategoryModel model) {
		SeriesRender seriesRender = new SeriesRender(getType() ,isUseChartFgAlpha(), Events.isListened(Fusionchart.this, Events.ON_CLICK, false));
		CategoryRender categoryRender = new CategoryRender();
		
		boolean isPutCatesDone = false;
		Collection cates = model.getCategories();
		for (final Iterator it = model.getSeries().iterator(); it.hasNext();) {
			Comparable series = (Comparable) it.next();
			seriesRender.renderSeries(series);
			
			for (final Iterator it2 = cates.iterator(); it2.hasNext();) {
				Comparable category = (Comparable) it2.next();
				//categories
				if (!isPutCatesDone)
					categoryRender.renderCategory(category);
				
				seriesRender.renderSeriesSet(series, category, model);
			}
			
			seriesRender.renderSeriesEnd();
			isPutCatesDone = true;
		}
		
		return categoryRender.finishRender().append(seriesRender.getSb().toString()).toString();
	}

	/**
	 * transfer a XYModel into JFreeChart XYSeriesCollection.
	 */
	private String XYModelToXYDataset(XYModel model) {
		Collection serieses = model.getSeries();
		
		Set xnumSet = new TreeSet();
		Map xnumMap = new LinkedHashMap();
		
		XYModelSeriesRender seriesRender = new XYModelSeriesRender(getType() ,isUseChartFgAlpha(), Events.isListened(Fusionchart.this, Events.ON_CLICK, false));
		CategoryRender categoryRender = new CategoryRender();
		
		// record total length
		for (final Iterator it = serieses.iterator(); it.hasNext();) {
			final Comparable series = (Comparable) it.next();
			final int size = model.getDataCount(series);
			Map valuesMap = new TreeMap();
			for(int j = 0; j < size; ++j) {
				Number x = model.getX(series, j);
				xnumSet.add(x);
				valuesMap.put(x, new Integer(j));
			}
			xnumMap.put(series, valuesMap);
		}
		
		int index = 0, missCount = 0;
		boolean isPutCatesDone = false;
		for (final Iterator it = serieses.iterator(); it.hasNext();) {
			final Comparable series = (Comparable) it.next();
			seriesRender.renderSeries(series);
			
			Number prevY = new Integer(0);
			Iterator valuesMapIt = ((Map) xnumMap.get(series)).entrySet().iterator();
			for (final Iterator xnumSetIt = xnumSet.iterator(); xnumSetIt.hasNext();) {
				Number x1 = (Number) xnumSetIt.next();
				
				if (!isPutCatesDone)
					categoryRender.renderCategory(x1);
				
				if (!valuesMapIt.hasNext()) 
					if (!isPutCatesDone)
						continue;
					else break;
				
				Map.Entry entry = (Map.Entry) valuesMapIt.next();
				index = Integer.parseInt(entry.getValue()+"");
				Number x = (Number) entry.getKey();
				Number y = model.getY(series, index);
				
				if (Objects.equals(x1, x)) {//match
					seriesRender.renderSeriesSet(series, index, model);
				} else {
					do { //count miss number
						missCount++;
						x1 = (Number) xnumSetIt.next();
						if (!isPutCatesDone)
							categoryRender.renderCategory(x1);
					} while (!Objects.equals(x1, x));
					
					double y1 = y.doubleValue();
					double y2 = prevY.doubleValue();
					double offset = (y1 - y2) / (missCount + 1);
					int count = missCount;
					
					while (missCount > 0) {//add miss numbers
						missCount--;
						String value = y2 == 0 ? "0":
								y2 + offset * (count - missCount)+"";
						seriesRender.renderSeriesSet(value);
					}
					
					seriesRender.renderSeriesSet(series, index, model);
				}
				prevY = y;
			}
			seriesRender.renderSeriesEnd();
			isPutCatesDone = true;
			missCount = index = 0;
		}
		
		return categoryRender.finishRender().append(seriesRender.getSb().toString()).toString();
	}

	/**
	 * transfer a XYModel into JFreeChart DefaultTableXYDataset.
	 */
	private String XYModelToTableXYDataset(XYModel model) {
		StringBuffer sb = new StringBuffer();
		for (final Iterator it = model.getSeries().iterator(); it.hasNext();) {
			final Comparable series = (Comparable) it.next();
//			XYSeries xyser = new XYSeries(series, false, false);
			final int size = model.getDataCount(series);
			for(int j = 0; j < size; ++j) {
				
//				xyser.add(model.getX(series, j), model.getY(series, j), false);
			}
//			dataset.addSeries(xyser);
		}
		return sb.toString();
	}
	
	/**
	 * transfer a GanttModel into JFreeChart GanttDataset.
	 */
	private String GanttModelToGanttDataset(GanttModel model) {
		StringBuffer sb = new StringBuffer();
		final Comparable[] allseries = model.getAllSeries();
		final int sz = allseries.length;
		for (int j = 0; j < sz; ++j) {
			final Comparable series = allseries[j];
//			final TaskSeries taskseries = new TaskSeries((String) series);
//			final GanttTask[] tasks = model.getTasks(series);
//			final int tsz = tasks.length;
//			for (int k = 0; k < tsz; ++k) {
//				final GanttTask task = tasks[k];
//				final Task jtask = newTask(task);
//				addSubtask(jtask, task);
//				taskseries.add(jtask); 
//			}
//			dataset.add(taskseries);
		}
		
		return sb.toString();
	}
	
	/** pie chart */
	/** pie chart */
	private class Pie extends ChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToPieDataset((CategoryModel)_model));
			if (_model instanceof PieModel) 
				return createChart(PieModelToPieDataset((PieModel) _model));
			
			throw new UiException("The model of pie chart must be a org.zkoss.zul.PieModel or a org.zkoss.zul.CategoryModel");
		}
		protected void renderProperties(StringBuffer sb) {
			sb.append(" pieFillAlpha='").append(toFusionchartAlpha(getFgAlpha()))
				.append("'  shownames='1'");
		}
	}
	
	/** bar chart */
	private class Bar extends AbstractChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToCategoryDataset((CategoryModel)_model));
			if (_model instanceof XYModel) 
				return createChart(XYModelToXYDataset((XYModel) _model));
			
			throw new UiException("The model of bar chart must be a org.zkoss.zul.CategoryModel or a org.zkoss.zul.XYModel");
		}
		protected void renderProperties(StringBuffer sb){
			
		}
	}
	
	/** area chart */
	private class AreaImpl extends AbstractChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToCategoryDataset((CategoryModel)_model));
			if (_model instanceof XYModel) 
				return createChart(XYModelToXYDataset((XYModel) _model));
			
			throw new UiException("The model of area chart must be a org.zkoss.zul.CategoryModel or a org.zkoss.zul.XYModel");
		}
	}

	/** line chart */
	private class Line extends AbstractChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToCategoryDataset((CategoryModel)_model));
			if (_model instanceof XYModel) 
				return createChart(XYModelToXYDataset((XYModel) _model));
			
			throw new UiException("The model of line chart must be a org.zkoss.zul.CategoryModel or a org.zkoss.zul.XYModel");
		}
	}
	
	/** stackedbar chart */
	private class StackedBar extends AbstractChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToCategoryDataset((CategoryModel)_model));
			
			throw new UiException("The model of stacked_bar chart must be a org.zkoss.zul.CategoryModel");
		}
	}
	
	/** stackedarea chart */
	private class StackedArea extends AbstractChartImpl {
		public String createChartXML() {
			if (_model instanceof CategoryModel)
				return createChart(CategoryModelToCategoryDataset((CategoryModel)_model));
			else if (_model instanceof XYModel)
				return createChart(XYModelToTableXYDataset((XYModel)_model));
			
			throw new UiException("The model of stacked_area chart must be a org.zkoss.zul.CategoryModel or a org.zkoss.zul.XYModel");
		}
	}
	
	/** gantt chart
	 */
	private class Gantt extends ChartImpl {
		public String createChartXML() {
			if (_model instanceof GanttModel)
				return createChart(GanttModelToGanttDataset((GanttModel)_model));
			
			throw new UiException("The model of gantt chart must be a org.zkoss.zul.GanttModel");
		}
	}
	
	private int toFusionchartAlpha(int alpha) {
		return new Float(alpha*100/255).intValue();
	}
	
	private String toFusionchartColor(String color) {
		if (Strings.isBlank(color))
			return null;
		if (color.length() != 7 || !color.startsWith("#")) 
			throw new UiException("Incorrect color format (#RRGGBB) : " + color);
		
		if (color.startsWith("#"))
			return color.substring(1);
		return color;
	}
	
	private String toFusionchartAttr(String name, String value) {
		if (!Strings.isBlank(value)) {
			StringBuffer sb = new StringBuffer(" ");
			sb.append(name).append("='").append(value).append("'");
			return sb.toString();
		}
		
		return "";
	}
	
	/**
	 * mark a draw flag to inform that this Chart needs update.
	 */
	protected void smartDrawChart() {
		if (!_smartDrawChart) { 
			_smartDrawChart = true;
			Events.postEvent("onSmartDrawChart", this, null);
		}
	}

	public String getZclass() {
		return _zclass != null ? _zclass: "z-fusionchart";
	}
	
	//Cloneable//
	public Object clone() {
		final Fusionchart clone = (Fusionchart)super.clone();

		clone._smartDrawChartListener = null;
		clone._smartDrawChart = false;
		clone.init();
		clone.doSmartDraw();
		if (clone._model != null) {
			clone._dataListener = null;
			clone.initDataListener();
		}
		
		return clone;
	}
	
	public void service(AuRequest request, boolean everError) {
		final String cmd = request.getCommand();
		if (cmd.equals(Events.ON_CLICK)) {
			final Map data = request.getData();
			ChartModel model = getModel();
			int cateIndex = AuRequests.getInt(data, "category", 0, true);
			
			if (model instanceof CategoryModel) {
				
				int seriIndex = AuRequests.getInt(data, "series", 0, true);
				CategoryModel cateModel = (CategoryModel) model;
				Comparable series = cateModel.getSeries(seriIndex);
				Comparable category	= cateModel.getCategory(cateIndex);
				
				data.put("series", series);
				data.put("category", category);
				data.put("value", ((CategoryModel) model).getValue(series, category));
				
			} else if (model instanceof PieModel) {
				PieModel pieModel = (PieModel) model;
				Comparable category	= pieModel.getCategory(cateIndex);
				
				data.put("category", category);
				data.put("value", pieModel.getValue(category));
				
			} else if (model instanceof XYModel) {
				XYModel xyModel = (XYModel) model;
				int seriIndex = AuRequests.getInt(data, "series", 0, true);
				Comparable series = xyModel.getSeries(seriIndex);
				
				data.put("series", series);
				data.put("x", xyModel.getX(series, cateIndex));
				data.put("y", xyModel.getY(series, cateIndex));
				
			} else if (model instanceof GanttModel) {
				
			}
			Events.postEvent(new Event(cmd, this, data));
		} else
			super.service(request, everError);
	}
	
	protected void renderProperties(ContentRenderer renderer)
			throws IOException {
		super.renderProperties(renderer);
		render(renderer, "type", _type);
		render(renderer, "threeD", _threeD);
		render(renderer, "orient", _orient);
	}
	
	/**
	 * A Fusionchart chart data.
	 * 
	 * @author jimmyshiau
	 */
	/* package */static class FusionchartData implements java.io.Serializable {
		private static final long serialVersionUID = 20110104121812L;
		private Number value;
		private String color;
		private int alpha = 255;
		private String link;

		public FusionchartData(Number value) {
			super();
			this.value = value;
		}

		public FusionchartData(Number value, String color) {
			super();
			this.value = value;
			this.color = color;
		}

		public FusionchartData(Number value, String color, String link) {
			super();
			this.value = value;
			this.color = color;
			this.link = link;
		}

		public FusionchartData(Number value, String color, String link,
				int alpha) {
			super();
			this.value = value;
			this.color = color;
			this.link = link;
			this.alpha = alpha;
		}

		/**
		 * Returns the numerical value for the set of data according to which
		 * the chart would be built for the concerned set of data.
		 * @return Number
		 */
		public Number getValue() {
			return value;
		}

		/**
		 * Sets the numerical value for the set of data according to which
		 * the chart would be built for the concerned set of data.
		 * @param value
		 */
		public void setValue(Number value) {
			this.value = value;
		}
		
		/**
		 * Returns the color for the concerned set of data in which it would
		 * appear in the graph.
		 * @return String
		 */
		public String getColor() {
			return color;
		}

		/**
		 * Sets the color for the concerned set of data in which it would
		 * appear in the graph.
		 * @param color
		 */
		public void setColor(String color) {
			this.color = color;
		}
		
		/**
		 * Returns the transparency of a data set. The range for this attribute
		 * is 0 to 255.
		 * <p>
		 * Default: 255.
		 * @return String
		 */
		public int getAlpha() {
			return alpha;
		}

		/**
		 * Sets the transparency of a data set. The range for this attribute
		 * is 0 to 255.
		 * <p>
		 * Default: 255.
		 * @param alpha
		 */
		public void setAlpha(int alpha) {
			this.alpha = alpha;
		}
		
		/**
		 * Returns the hotspots in your graph. The hotspots are links over the
		 * data sets.
		 * @return String
		 */
		public String getLink() {
			return link;
		}

		/**
		 * Sets  the hotspots in your graph. The hotspots are links over the
		 * data sets.
		 * @param link
		 */
		public void setLink(String link) {
			this.link = link;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + alpha;
			result = prime * result + ((color == null) ? 0 : color.hashCode());
			result = prime * result + ((link == null) ? 0 : link.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			FusionchartData other = (FusionchartData) obj;
			if (alpha != other.alpha)
				return false;
			if (color == null) {
				if (other.color != null)
					return false;
			} else if (!color.equals(other.color))
				return false;
			if (link == null) {
				if (other.link != null)
					return false;
			} else if (!link.equals(other.link))
				return false;
			if (value == null) {
				if (other.value != null)
					return false;
			} else if (!value.equals(other.value))
				return false;
			return true;
		}

		public String toString() {
			return "FusionchartData [value=" + value + ", color=" + color
					+ ", link=" + link + ", alpha=" + alpha + "]";
		}
	}
	
	/**
	 * Class to extend the number of Colors available to the charts. This
	 * extends the java.awt.Color object and extends the number of final
	 * Colors publically accessible.
	 */
	public static class ChartColor extends Color {

	    /** A very dark red color. */
	    public static final Color VERY_DARK_RED = new Color(0x80, 0x00, 0x00);

	    /** A dark red color. */
	    public static final Color DARK_RED = new Color(0xc0, 0x00, 0x00);

	    /** A light red color. */
	    public static final Color LIGHT_RED = new Color(0xFF, 0x40, 0x40);

	    /** A very light red color. */
	    public static final Color VERY_LIGHT_RED = new Color(0xFF, 0x80, 0x80);

	    /** A very dark yellow color. */
	    public static final Color VERY_DARK_YELLOW = new Color(0x80, 0x80, 0x00);

	    /** A dark yellow color. */
	    public static final Color DARK_YELLOW = new Color(0xC0, 0xC0, 0x00);

	    /** A light yellow color. */
	    public static final Color LIGHT_YELLOW = new Color(0xFF, 0xFF, 0x40);

	    /** A very light yellow color. */
	    public static final Color VERY_LIGHT_YELLOW = new Color(0xFF, 0xFF, 0x80);

	    /** A very dark green color. */
	    public static final Color VERY_DARK_GREEN = new Color(0x00, 0x80, 0x00);

	    /** A dark green color. */
	    public static final Color DARK_GREEN = new Color(0x00, 0xC0, 0x00);

	    /** A light green color. */
	    public static final Color LIGHT_GREEN = new Color(0x40, 0xFF, 0x40);

	    /** A very light green color. */
	    public static final Color VERY_LIGHT_GREEN = new Color(0x80, 0xFF, 0x80);

	    /** A very dark cyan color. */
	    public static final Color VERY_DARK_CYAN = new Color(0x00, 0x80, 0x80);

	    /** A dark cyan color. */
	    public static final Color DARK_CYAN = new Color(0x00, 0xC0, 0xC0);

	    /** A light cyan color. */
	    public static final Color LIGHT_CYAN = new Color(0x40, 0xFF, 0xFF);

	    /** Aa very light cyan color. */
	    public static final Color VERY_LIGHT_CYAN = new Color(0x80, 0xFF, 0xFF);

	    /** A very dark blue color. */
	    public static final Color VERY_DARK_BLUE = new Color(0x00, 0x00, 0x80);

	    /** A dark blue color. */
	    public static final Color DARK_BLUE = new Color(0x00, 0x00, 0xC0);

	    /** A light blue color. */
	    public static final Color LIGHT_BLUE = new Color(0x40, 0x40, 0xFF);

	    /** A very light blue color. */
	    public static final Color VERY_LIGHT_BLUE = new Color(0x80, 0x80, 0xFF);

	    /** A very dark magenta/purple color. */
	    public static final Color VERY_DARK_MAGENTA = new Color(0x80, 0x00, 0x80);

	    /** A dark magenta color. */
	    public static final Color DARK_MAGENTA = new Color(0xC0, 0x00, 0xC0);

	    /** A light magenta color. */
	    public static final Color LIGHT_MAGENTA = new Color(0xFF, 0x40, 0xFF);

	    /** A very light magenta color. */
	    public static final Color VERY_LIGHT_MAGENTA = new Color(0xFF, 0x80, 0xFF);

	    /**
	     * Creates a Color with an opaque sRGB with red, green and blue values in
	     * range 0-255.
	     *
	     * @param r  the red component in range 0x00-0xFF.
	     * @param g  the green component in range 0x00-0xFF.
	     * @param b  the blue component in range 0x00-0xFF.
	     */
	    public ChartColor(int r, int g, int b) {
	        super(r, g, b);
	    }

	    /**
	     * Convenience method to return an array of <code>Paint</code> objects that
	     * represent the pre-defined colors in the <code>Color<code> and
	     * <code>ChartColor</code> objects.
	     *
	     * @return An array of objects with the <code>Paint</code> interface.
	     */
	    public static Paint[] createDefaultPaintArray() {

	        return new Paint[] {
	            new Color(0xFF, 0x55, 0x55),
	            new Color(0x55, 0x55, 0xFF),
	            new Color(0x55, 0xFF, 0x55),
	            new Color(0xFF, 0xFF, 0x55),
	            new Color(0xFF, 0x55, 0xFF),
	            new Color(0x55, 0xFF, 0xFF),
	            Color.pink,
	            Color.gray,
	            ChartColor.DARK_RED,
	            ChartColor.DARK_BLUE,
	            ChartColor.DARK_GREEN,
	            ChartColor.DARK_YELLOW,
	            ChartColor.DARK_MAGENTA,
	            ChartColor.DARK_CYAN,
	            Color.darkGray,
	            ChartColor.LIGHT_RED,
	            ChartColor.LIGHT_BLUE,
	            ChartColor.LIGHT_GREEN,
	            ChartColor.LIGHT_YELLOW,
	            ChartColor.LIGHT_MAGENTA,
	            ChartColor.LIGHT_CYAN,
	            Color.lightGray,
	            ChartColor.VERY_DARK_RED,
	            ChartColor.VERY_DARK_BLUE,
	            ChartColor.VERY_DARK_GREEN,
	            ChartColor.VERY_DARK_YELLOW,
	            ChartColor.VERY_DARK_MAGENTA,
	            ChartColor.VERY_DARK_CYAN,
	            ChartColor.VERY_LIGHT_RED,
	            ChartColor.VERY_LIGHT_BLUE,
	            ChartColor.VERY_LIGHT_GREEN,
	            ChartColor.VERY_LIGHT_YELLOW,
	            ChartColor.VERY_LIGHT_MAGENTA,
	            ChartColor.VERY_LIGHT_CYAN
	        };
	    }
	}
	
	/**
     * Returns the next color in the sequence.
     *
     * @return The color in RRGGBB format (hexdecimal).
     */
    public String getNextColor() {
        Color result
            = (Color) DEFAULT_PAINT_SEQUENCE[this._paintIndex++ % this.DEFAULT_PAINT_SEQUENCE.length];
        return Integer.toHexString(result.getRGB()).substring(2).toUpperCase();
        
    }
    
    public static String encodeRGB(Color color){
        if(null == color) {
          throw new IllegalArgumentException("NULL_COLOR_PARAMETER_ERROR_2");
        }
        return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
      }
}