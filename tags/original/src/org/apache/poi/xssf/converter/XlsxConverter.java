package org.apache.poi.xssf.converter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.poi.hwpf.converter.HtmlDocumentFacade;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import cc.mask.utils.StringUtil;

public class XlsxConverter {
	
	private XSSFWorkbook x;
	private HtmlDocumentFacade htmlDocumentFacade;
	private Element page;
	private String output;
	
	
	private XlsxConverter(String filePath, String output) throws IOException, InvalidFormatException, ParserConfigurationException{
		this.output = output;
		
		OPCPackage op = OPCPackage.open(filePath);
		x = new XSSFWorkbook(op);
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		this.htmlDocumentFacade = new HtmlDocumentFacade(document);
		
		Element window = htmlDocumentFacade.createBlock();
		window.setAttribute("id", "window");
		page = htmlDocumentFacade.createBlock();
		page.setAttribute("id", "page");
		
		window.appendChild(page);
		htmlDocumentFacade.getBody().appendChild(window);
	}
	
	public static void main(String[] args) throws InvalidFormatException, IOException, ParserConfigurationException, TransformerException {
		String name = "test";
		XlsxConverter.convert("c:/poi/" +name+ ".xlsx", "c:/poi/x/" +name+ ".html");
	}
	/**
	 * 
	 * @param filePath
	 * @param output
	 * @throws InvalidFormatException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 */
	public static void convert(String filePath, String output) throws InvalidFormatException, IOException, ParserConfigurationException, TransformerException{
		XlsxConverter converter = new XlsxConverter(filePath, output);
		
		Integer sheetNum = converter.x.getNumberOfSheets();
		for(int i = 0; i < sheetNum; i ++){
			XSSFSheet sheet = converter.x.getSheet(converter.x.getSheetName(i));
			String sheetName =  converter.x.getSheetName(i);
			System.err.println("starting process sheet : " +sheetName);
			// add sheet title
			{
				Element title = converter.htmlDocumentFacade.createHeader2();
				title.setTextContent(sheetName);
				converter.page.appendChild(title);
			}
			converter.processSheet(converter.page, sheet);
		}
		
		converter.htmlDocumentFacade.updateStylesheet();
		converter.saveAsHtml(output, converter.htmlDocumentFacade.getDocument());
	}

	private void processSheet(Element container, XSSFSheet sheet) {
		// add sheet title
		
		Element table = htmlDocumentFacade.createTable();
//		htmlDocumentFacade.addStyleClass(table, "table", "border:1");
		table.setAttribute("border", "1");
		table.setAttribute("cellpadding", "2");
		table.setAttribute("cellspacing", "0");
		table.setAttribute("style", "border-collapse: collapse;");
		
		Iterator<Row> rows = sheet.iterator();
		while(rows.hasNext()){
			Row row = rows.next();
			if(row instanceof XSSFRow)
				processRow(table, (XSSFRow)row);
		}
		
		container.appendChild(table);
		
	}

	private void processRow(Element table, XSSFRow row) {
		Element tr = htmlDocumentFacade.createTableRow();
		
		Iterator<Cell> cells = row.cellIterator();
		while(cells.hasNext()){
			Cell cell = cells.next();
			if(cell instanceof XSSFCell)
				processCell(tr, (XSSFCell)cell);
		}
		
		table.appendChild(tr);
	}

	private void processCell(Element tr, XSSFCell cell) {
		Element td = htmlDocumentFacade.createTableCell();
		Object value ;
		switch(cell.getCellType()){
		case Cell.CELL_TYPE_BLANK : value = "\u00a0";	break;
		case Cell.CELL_TYPE_NUMERIC : value = cell.getNumericCellValue();	break;
		case Cell.CELL_TYPE_BOOLEAN : value =  cell.getBooleanCellValue();	break;
		case Cell.CELL_TYPE_FORMULA : value = cell.getNumericCellValue();	break;
		default : value = cell.getRichStringCellValue();	break;
		}
		if(value instanceof XSSFRichTextString){
			processCellStyle(td, cell.getCellStyle(), (XSSFRichTextString)value);
			td.setTextContent(value.toString());
		}
		else{
			processCellStyle(td, cell.getCellStyle(), null);
			td.setTextContent(value.toString());
		}
		System.err.println(value);
		tr.appendChild(td);	
	}

	private void processCellStyle(Element td, XSSFCellStyle style, XSSFRichTextString rts) {
		StringBuilder sb = new StringBuilder();
		
		if(rts != null){
			XSSFFont font = rts.getFontOfFormattingRun(1);
			if(font != null){
				sb.append("font-family:").append(font.getFontName()).append(";");
//				sb.append("color:").append(font.getColor() ).append(";");
				sb.append("font-size:").append(font.getFontHeightInPoints()).append("pt;");
				if(font.getXSSFColor()!= null){
					String color = font.getXSSFColor().getARGBHex().substring(2);
					sb.append("color:#").append(color).append(";");
				}
				if(font.getItalic())
					sb.append("font-style:italic;");
				if(font.getBold())
					sb.append("font-weight:").append(font.getBoldweight() ).append(";");
				if(font.getStrikeout()){
					sb.append("text-decoration:underline;");
				}
				
			}
		}
		if(style.getAlignment() != 1){
			switch(style.getAlignment()){
			case 2: sb.append("text-align:").append("center;");	break;
			case 3: sb.append("text-align:").append("right;");	break;
			}
		}
/*		if(style.getBorderBottom() != 0 )
			sb.append("border-bottom:").append(style.getBorderBottom()).append("px;");
		if( style.getBorderLeft() != 0 )
			sb.append("border-left:").append(style.getBorderLeft()).append("px;");
		if(style.getBorderTop() != 0 )
			sb.append("border-top:").append(style.getBorderTop()).append("px;");
		if(style.getBorderRight() != 0 )
			sb.append("border-right:").append(style.getBorderRight()).append("px;");
		if(style.getFillBackgroundXSSFColor()!=null){
			XSSFColor color = style.getFillBackgroundXSSFColor();
		}*/
		
//		System.out.println(style.getFillBackgroundXSSFColor());
		if( style.getFillBackgroundXSSFColor()!= null){
			sb.append("background:#ccc;");
		}
		htmlDocumentFacade.addStyleClass(td, "td", sb.toString());
	}

	/**
	 * @param output
	 * @param document
	 * @throws IOException
	 * @throws TransformerException
	 */
	private void saveAsHtml(String output, org.w3c.dom.Document document) throws IOException, TransformerException{
		
//		check path
		File folder = new File(StringUtil.getFilePath(output));	
		if(!folder.canRead()) 
			folder.mkdirs();
		folder = null;
		
		
		FileWriter out = new FileWriter( output );
        DOMSource domSource = new DOMSource(document );
        StreamResult streamResult = new StreamResult( out );

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        // TODO set encoding from a command argument
        serializer.setOutputProperty( OutputKeys.ENCODING, "UTF-8" );
        serializer.setOutputProperty( OutputKeys.INDENT, "yes" );
        serializer.setOutputProperty( OutputKeys.METHOD, "html" );
        serializer.setOutputProperty(OutputKeys.STANDALONE , "yes"); 
        serializer.setOutputProperty( OutputKeys.DOCTYPE_PUBLIC, "-//W3C//DTD HTML 4.01 Transitional//EN"); //
        serializer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.w3.org/TR/html4/strict.dtd");
        
        serializer.transform( domSource, streamResult );
        out.close();
	}
	
}
