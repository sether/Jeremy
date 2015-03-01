package com.jeremy;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to convert TableData into an XML Document
 * @author Scott Micklethwaite
 * @version 1.0
 */
public class XMLHandler {
	private final Double XML_VERSION = 1.0;
	private final String XML_ENCODING = "UTF-8";
	private final boolean FIELD_AS_ELEMENT = true;
	private XMLTag root;
	private TableData data;
	
	/**
	 * Default constructor for initializing an XMLHandler. Calls the createXMLObjects method.
	 * @param data The TableData object that will be converted to an XML file
	 */
	public XMLHandler(TableData data){
		this.data = data;
		createXMLObjects();
	}
	
	/**
	 * Creates an XML object structure using TableData. Will create row data as elements or attributes depending on
	 * the value of FIELD_AS_ELEMENT.
	 */
	public void createXMLObjects(){
		Object[][] tableData = data.getTableData();
		Object[] headings = data.getColumnHeader();
		int rowCount = tableData.length;
		int colCount = headings.length;
		int startLine = 0;
		
		//create base tag in xml hierarchy
		this.root = new XMLTag("Table");
		
		//iterate through 2d object array
		for(int i = startLine; i < rowCount; i++){
			//add new tag to root for each row
			XMLTag row = new XMLTag("Row");
			this.root.tags.add(row);
			
			//add each field as an element or as an attribute
			for(int j = 0; j < colCount; j++){
				if(FIELD_AS_ELEMENT){
					row.elements.add(new XMLElement(headings[j].toString(), tableData[i][j].toString()));
				} else {
					row.attribs.add(new XMLAttribute(headings[j].toString(), tableData[i][j].toString()));
				}
			}
		}
	}
	
	/**
	 * Constructs an xml document string and returns it. Build this string by creating a header then appending the
	 * xml document data to this.
	 * @return XML document contents as a string
	 */
	public String getXMLString(){
		String doc = "";
		doc += createXMLHeader() + "\n";
		doc += tagToString(root, 0);
		return doc;
	}
	
	/**
	 * Constructs an XSD schema that matches the structure of the XML string to be generated. Will output a slightly
	 * different structure as required by using rows as elements or attributes.
	 * @return XSD document matching the XML document
	 */
	public String getSchemaString(){
		String dataStruc;
		XMLTag container;
		
		//create base of structure
		XMLTag base = new XMLTag("xs:schema");
		base.attribs.add(new XMLAttribute("attributeFormDefault", "unqualified"));
		base.attribs.add(new XMLAttribute("elementFormDefault", "qualified"));
		base.attribs.add(new XMLAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema"));
		
		//non dynamic xml schema content
		XMLTag table = new XMLTag("xs:element");
		base.tags.add(table);
		table.attribs.add(new XMLAttribute("name", "Table"));
		
		XMLTag complex1 = new XMLTag("xs:complexType");
		table.tags.add(complex1);
		
		XMLTag sequence1 = new XMLTag("xs:sequence");
		complex1.tags.add(sequence1);
		
		XMLTag element1 = new XMLTag("xs:element");
		sequence1.tags.add(element1);
		element1.attribs.add(new XMLAttribute("name", "Row"));
		element1.attribs.add(new XMLAttribute("maxOccurs", "unbounded"));
		element1.attribs.add(new XMLAttribute("minOccurs", "0"));
		
		XMLTag complex2 = new XMLTag("xs:complexType");
		element1.tags.add(complex2);
		
		// creates different tag structure from this point depending on if fields should be elements or attributes
		if(FIELD_AS_ELEMENT){
			dataStruc = "element";
			
			container = new XMLTag("xs:sequence");
			complex2.tags.add(container);
		} else {
			dataStruc = "attribute";
			
			XMLTag simpleContent = new XMLTag("xs:simpleContent");
			complex2.tags.add(simpleContent);
			
			container = new XMLTag("xs:extension");
			container.attribs.add(new XMLAttribute("base", "xs:string"));
			simpleContent.tags.add(container);
		}
		
		// records field data types as pulled from column classes in TableData object
		for(int i = 0; i < data.getFields(); i++){
			XMLTag fieldElement = new XMLTag("xs:" + dataStruc);
			fieldElement.attribs.add(new XMLAttribute("type", ("xs:" + data.getColumnClasses()[i].getSimpleName().toLowerCase())));
			fieldElement.attribs.add(new XMLAttribute("name", data.getColumnHeader()[i]));
			container.tags.add(fieldElement);
		}
		
		//generate xml string and return
		return tagToString(base, 0);
	}
	
	/**
	 * Constructs an XML header for usage at the top of an XML document.
	 * @return an XML header
	 */
	private String createXMLHeader(){
		return "<?xml version=\"" + XML_VERSION + "\" encoding=\"" + XML_ENCODING + "\"?>";
	}
	
	/**
	 * Converts the contents of an XML tag to a string to be used in an XML document. Will create tags and their attributes
	 * and their child objects such as elements and child tags. Operates recursively when dealing with child tags.
	 * @param tag The XMLTag object to generate a string from.
	 * @param indent The initial tab spacing of this tag.
	 * @return A String containing formatted XML data
	 */
	
	private String tagToString(XMLTag tag, int indent){
		String all = "";
		int ind = indent;
		boolean singleLine = (tag.elements.size() == 0 && tag.tags.size() == 0);
		
		String line = "";
		//format tag start and add attributes
		line = addTab(ind);
		line = "<" + tag.name;
		line += attribToString(tag.attribs);
		
		if(singleLine){ // make single line tag if no child contents exist
			//close start tag with
			line += "/>";
			all += addTab(ind) + line;
		} else {
			//close start tag
			line += ">";
			all += addTab(ind) + line + "\n";
			
			//insert elements
			for(XMLElement elem : tag.elements){
				line = addTab(ind + 1);
				line += "<" + elem.name + attribToString(elem.attribs) + ">";
				line += elem.value;
				line += "</" + elem.name + ">";
				all += line + "\n";
			}
			
			//child tags
			for(XMLTag child : tag.tags){
				all += tagToString(child, indent + 1) + "\n";
			}
			
			//close attribute
			line = "</" + tag.name + ">";
			all += addTab(ind) + line;
		}
		
		return all;
	}
	
	/**
	 * Formats a list of XMLAttribute objects into a neatly spaced string. Spaces multiple values apart. This is used
	 * for neatly adding XML attributes to a tag.
	 * @param a The list of XMLAttribute objects to use
	 * @return a string containing XML style attributes.
	 */
	private String attribToString(List<XMLAttribute> a){
		String s = "";
		XMLAttribute att;
		if(a.size() > 0){
			s = " ";
			for(int i = 0; i < a.size(); i++){
				att = a.get(i);
				s += att.name + "=" + "\"" + att.value + "\"";
				if(i < a.size() - 1){ // space between attributes if not the last one
					s += " ";
				}
			}
		}
		return s;
	}
	
	/**
	 * Method for creating a number of tab strings to insert into XML documents.
	 * @param indent the number of tabs to generate
	 * @return A string containing the desired number of tabs
	 */
	private String addTab(int indent){
		String s = "";
		for(int i = 0; i < indent; i++){
			s = "\t" + s;
		}
		return s;
	}
	
	/**
	 * A class to represent an XML tag object. XML tags may contain child tags, elements and attributes. A tag must also
	 * contain a name.
	 * @author Scott Micklethwaite
	 * @version 1.0
	 */
	private class XMLTag{
		private List<XMLTag> tags;
		private List<XMLElement> elements;
		private List<XMLAttribute> attribs;
		private String name;
		
		public XMLTag(String name){
			this.name = name;
			this.tags = new ArrayList<XMLTag>();
			this.elements = new ArrayList<XMLElement>();
			this.attribs = new ArrayList<XMLAttribute>();
		}
	}
	
	/**
	 * A class for representing an XML element. An element must have a name and a value. It may additionally contain
	 * a list of attributes.
	 * @author Scott Micklethwaite
	 * @version 1.0
	 */
	private class XMLElement{
		private String name;
		private String value;
		private List<XMLAttribute> attribs;
		
		public XMLElement(String name, String value){
			this.name = name;
			this.value = value;
			this.attribs = new ArrayList<XMLAttribute>();
		}
	}
	
	/**
	 * A class for representing an XML attribute. Must contain a name and a value.
	 * @author Scott Micklethwaite
	 * @version 1.0
	 */
	private class XMLAttribute{
		private String name;
		private String value;
		
		public XMLAttribute(String name, String value){
			this.name = name;
			this.value = value;
		}
	}
}
