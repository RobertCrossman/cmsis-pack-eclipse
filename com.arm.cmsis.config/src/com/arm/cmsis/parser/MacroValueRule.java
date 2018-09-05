/*******************************************************************************
 * Copyright (c) 2016 ARM Ltd. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - initial API and implementation
 * Wind River Systems, Inc. - bug fixes
 * ARM Ltd and ARM Germany GmbH - application-specific implementation
 *******************************************************************************/
package com.arm.cmsis.parser;

import org.eclipse.jface.text.rules.ICharacterScanner;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.SingleLineRule;
import org.eclipse.jface.text.rules.Token;

/**
 * Recognizes a #define statement's value
 */
public class MacroValueRule extends SingleLineRule {

	public MacroValueRule(IToken token) {
		super("#define", null, token);
	}

	
    static private boolean isWhitespace(int ch)
    {
        return (' ' == ch || ' ' == '\t');
    }
    
    static private boolean isIdentifierStartChar(int ch)
    {
        if ('a' <= ch && ch < 'z' || 'A' <= ch && ch <= 'Z' || ch == '_')
            return true;
        return false;
    }
    
    static private boolean isIdentifierChar(int ch)
    {
        if (isIdentifierStartChar(ch))
            return true;
        return ('0' <= ch && ch <= '9');
    }
    
    private String findIdentifier(ICharacterScanner scanner)
    {
        int ch;
        int unreadCount = 0;
        
        ch = scanner.read();
        unreadCount++;
        
        if (isIdentifierStartChar(ch))
        {
            StringBuffer sb = new StringBuffer();
            sb.append((char)ch);
            
            // pass thru any identifier characters
            ch = scanner.read();
            unreadCount++;
            while (isIdentifierChar(ch)) {
                sb.append((char)ch);
                ch = scanner.read();
                unreadCount++;
            }
            
            // push back the last non-identifier character
            scanner.unread();
            
            // create a string from the accumulated characters                
            String identifier = new String(sb);
            identifier.intern();
            
            return identifier;
        }
        
        // push back the characters we read but didn't use
        while (unreadCount-- > 0)
            scanner.unread();
        
        // indicate failure
        return null;
    }
    
	@Override
	public IToken evaluate(ICharacterScanner scanner) 
	{
		int unreadCount = 0;
        int ch;


        // match the characters in "#define" 
		if (!sequenceDetected(scanner, "#define".toCharArray(), false))
		    return Token.UNDEFINED;
		
		// find at least 1 whitespace char
        ch = scanner.read();
        unreadCount++;
        if (isWhitespace(ch))
        {
            // pass thru any additional whitespace
            while (isWhitespace(ch))
    		{
    		    ch = scanner.read();
    		    unreadCount++;
    		}
            scanner.unread();

            // macro name - chars to build a C identifier
    		String macroName = findIdentifier(scanner);
    		if (macroName != null)
    		{
                // find at least 1 whitespace char
                ch = scanner.read();
                unreadCount++;
                if (isWhitespace(ch))
                {
                    // pass thru any additional whitespace
                    do {
                        ch = scanner.read();
                        unreadCount++;
                    } while (isWhitespace(ch));
                    scanner.unread();
                    
                    // macro value - chars to build a C identifier
                    String macroValue = findIdentifier(scanner);
                    if (macroValue != null)
                    {
                        // return the token which indicates we successfully found the pattern 
                        IToken token = getSuccessToken();
                        return token;
                    }
                    
                    // skip optional single left-paren
                    ch = scanner.read();
                    unreadCount++;
                    if (ch != '(') {
                        scanner.unread();
                        unreadCount--;
                    }
                    
                    // not an identifier, search for a number
                    NumberRule numberRule = new NumberRule(new Token(ConfigWizardScanner.CONFIG_MACROVALUE));
                    IToken tkn = numberRule.evaluate(scanner);
                    if (tkn != Token.UNDEFINED)
                        return tkn;
                }
                
            }
		}


        // failed to identify the patter, push back the scanned characters  
        do {
            scanner.unread();
        } while (--unreadCount > 0);

        return Token.UNDEFINED;

	}

}