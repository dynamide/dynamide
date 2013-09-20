package com.dynamide.util;

import org.apache.xerces.parsers.SAXParser;

import org.apache.xerces.util.SymbolTable;

import org.apache.xerces.xni.grammars.XMLGrammarPool;

import org.apache.xerces.xni.parser.XMLParserConfiguration;

import org.xml.sax.EntityResolver;

//import org.apache.xerces.impl.Constants;
//import org.apache.xerces.util.ObjectFactory;



public class DynamideSAXParser
    extends SAXParser {

        //only 2 and 5 seem to get called. 8/24/2002 9:12AM

    /**
     * Constructs a SAX parser using the specified parser configuration.
     */
    public DynamideSAXParser(XMLParserConfiguration config) {
        super(config);
//System.out.println("1");
    } // <init>(XMLParserConfiguration)

    /**
     * Constructs a SAX parser using the dtd/xml schema parser configuration.
     */
    public DynamideSAXParser() {
        super(null, null);
//System.out.println("2");
    }

    /**
     * Constructs a SAX parser using the specified symbol table.
     */
    public DynamideSAXParser(SymbolTable symbolTable) {
        super(symbolTable, null);
//System.out.println("3");
    }

    /**
     * Constructs a SAX parser using the specified symbol table and
     * grammar pool.
     */
    public DynamideSAXParser(SymbolTable symbolTable, XMLGrammarPool grammarPool) {
        super(symbolTable, grammarPool);
//System.out.println("4");
    }

    private EntityResolver m_EntityResolver = null;

    public void setEntityResolver(EntityResolver resolver) {
//System.out.println("5");
        m_EntityResolver = resolver;

    }
    public EntityResolver getEntityResolver() {
//System.out.println("6");
        return m_EntityResolver;
    }

}
