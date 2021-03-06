/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.swip.nlToPivot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Murloc
 */
public class NlToPivotPreParser 
{
    
    //private static String tokensPreParser = "/Users/Murloc/Documents/IRIT/svn/swip/NlToPivot/resources/preParser/";
    
    private String tokensPreParser = "preParser/";
    
    private boolean startMaximum = false;
    private boolean startMinimum = false;
    private boolean startAverage = false;
    private boolean startSum = false;
    private boolean count = false;
    private boolean maximum = false;
    private boolean minimum = false;
    private boolean average = false;
    private boolean sum = false;
    private boolean moreThan = false;
    private boolean lessThan = false;
    
    private String query;
    private String adaptedQuery;
    private String lang;
    
    private HashMap<String, String> importantWord;
    
    public NlToPivotPreParser(String query, String lang)
    {
        this.query = query.trim();
        this.adaptedQuery = query;
        boolean aggBeginFound = false;
        boolean aggFound = false;
        this.importantWord = new HashMap<String, String>();
        
        
        if(lang.compareTo("fr") == 0)
        {
            this.tokensPreParser += "fr/";
            this.adaptedQuery = this.removeLittlePronouns(this.adaptedQuery);
        }
        else
            this.tokensPreParser += "en/";
        
        System.out.println("PreParser process : "+query+" ... ");
        
        adaptedQuery = this.clearImportantWords(adaptedQuery);
        
        System.out.println("PreParser without important words : "+this.adaptedQuery);

        File f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"list").getPath());
        this.parseToken(f, "", true);
        
        if(!aggBeginFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"count").getPath());
            aggBeginFound = this.count = this.parseToken(f, "", true);
        }
        
        if(!aggBeginFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"maximum").getPath());
            aggBeginFound = this.startMaximum = this.parseToken(f, "", true);
        }
        
        if(!aggBeginFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"minimum").getPath());
            aggBeginFound = this.startMinimum = this.parseToken(f, "", true);
        }
        
        if(!aggBeginFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"average").getPath());
            aggBeginFound = this.startAverage = this.parseToken(f, "", true);
        }
        
        if(!aggBeginFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"sum").getPath());
            aggBeginFound = this.startSum = this.parseToken(f, "", true);
        }
        
        if(!aggFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"maximum").getPath());
            aggFound = this.maximum = this.parseToken(f, "");
        }
        
        if(!aggFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"minimum").getPath());
            aggFound = this.minimum = this.parseToken(f, "");
        }
        
        if(!aggFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"average").getPath());
            aggFound = this.average = this.parseToken(f, "");
        }
        
        if(!aggFound)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"sum").getPath());
            aggFound = this.sum = this.parseToken(f, "");
        }
        
        f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"moreThan").getPath());
        this.moreThan = this.parseToken(f, "");

        if(!this.moreThan)
        {
            f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"lessThan").getPath());
            this.lessThan = this.parseToken(f, "");
        }
        
        
        f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"with").getPath());
        this.parseToken(f, "with");
        
        f = new File(this.getClass().getClassLoader().getResource(this.tokensPreParser+"stopList").getPath());
        this.parseToken(f, "");
        
        adaptedQuery = this.restoreImportantWords(adaptedQuery);
        
        System.out.println("PreParser return : "+this.adaptedQuery);
    }
    
    private String clearImportantWords(String query)
    {
        String ret = query;
        //replace all important words (matched with the " character) with a token to avoid to process them
        Pattern p = Pattern.compile("\"([^\"]+)*\"");
        Matcher m = p.matcher(ret);
        int i = 0;
        while(m.find())
        {
            String founded = m.group();
            String token = "%"+(i++)+"%";
            ret = ret.replaceAll(founded, " "+token+" ");
            System.out.println("REPLACE : "+founded+" BY "+token);
            this.importantWord.put(token, founded.replaceAll("\"", ""));
        }
        
        return ret;
    }
    
    private String restoreImportantWords(String query)
    {
        String ret = query;
        for(String token : this.importantWord.keySet())
        {
            String words = this.importantWord.get(token);
            System.out.println("RESTORE : "+token+" BY "+words);
            ret = ret.replaceAll(token, words);
        }
        this.importantWord.clear();
        
        return ret;
    }
    
    private String removeLittlePronouns(String query)
    {
        String ret = query;
        ret = ret.replaceAll("[ldsj]'", "");
        return ret;
    }
    
    private boolean parseToken(File f, String replace)
    {
        return this.parseToken(f, replace, false);
    }
    
    private boolean parseToken(File f, String replace, boolean begin)
    {
        boolean encore = true;
        try 
        {
            Scanner s = new Scanner(f);
            while(s.hasNext() && encore)
            {
                String token = (begin? "": " ")+s.nextLine()+" ";
                if((!begin && this.adaptedQuery.contains(token)) || (begin && this.adaptedQuery.startsWith(token)))
                {
                    replace = ((!begin && replace.length() == 0)? " ": replace);
                    this.adaptedQuery = this.adaptedQuery.replaceAll(token, replace);
                    encore = false;
                }
            }
        } 
        catch (FileNotFoundException ex) 
        {
            System.err.println("Erreur lecture fichier");
        }
        return !encore;
    }
    
    public boolean getCount()
    {
        return this.count;
    }
    
    public boolean getMoreThan()
    {
        return this.moreThan;
    }
    
    public boolean getLessThan()
    {
        return this.lessThan;
    }
    
    public boolean getMaximum()
    {
        return this.maximum;
    }
    
    public boolean getMinimum()
    {
        return this.minimum;
    }
    public boolean getAverage()
    {
        return this.average;
    }
    
    public boolean getSum()
    {
        return this.sum;
    }
    
    
    public boolean getStartMaximum()
    {
        return this.startMaximum;
    }
    
    public boolean getStartMinimum()
    {
        return this.startMinimum;
    }
    public boolean getStartAverage()
    {
        return this.startAverage;
    }
    
    public boolean getStartSum()
    {
        return this.startSum;
    }
    
    public String getAdaptedQuery()
    {
        String ret = this.adaptedQuery.replaceAll("\\.", "");
        return NlToPivotUtils.wordsToDecimalNum(ret);
    }
}
