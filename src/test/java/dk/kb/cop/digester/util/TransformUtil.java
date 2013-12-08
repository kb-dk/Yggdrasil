package dk.kb.cop.digester.util;

import org.apache.log4j.Logger;




/**
 * Utility klasse der skal hjælpe med at parse data
 * fra cumulus, og skabe mods objekter. Hvis nogen regler
 * ikke er overholdt logges dette. Logmeddelelen skal 
 * kunne forstås af lægmand. 
 * 
 * Følgende regler skal understøttes og skal behandles:
 * <ul>
 *  <li>sprog:værdi</li>
 *  <li>&lt;&lt;the>> Golden Gun</li>
 *  <li>&lt;&lt;Golden=Shiny>> Gun (Kun eenpr. felt understøttes. </li>
 *  <li>Sprog skal overholde RFC4646</li>
 * </ul>
 * @author jac
 */
public class TransformUtil {

    private static String LANG_SEPARATOR = "|";

    /**
    * Logger object til denne klasse.
    */
    private static Logger logger = Logger.getLogger(TransformUtil.class
            .getPackage().getName());
    
    
    /**
     * 
     */
    private static final int IS_NON_TRANSLITERATION = 0;
    
    /**
     * 
     */
    private static final int IS_TRANSLITERATION_REX = 1;
    
    
    /**
     * 
     */
    private static final int IS_RSS = 2;
    /**
     * Dummy constructor, for at tilfredsstille checkstyle :-(.
     */
    protected TransformUtil(){}
    
    /**
     * 
     * @param val Cumulus raw xml value felt.
     * @return Værdien, med cumulus regler overholdt.
     */
    public static String getCumulusVal(String val){       
        if(val.indexOf(LANG_SEPARATOR)>-1){
            return applyRules(val.substring(val.indexOf(LANG_SEPARATOR)+1
                    , val.length()), IS_NON_TRANSLITERATION).trim();
        } else {
            return applyRules(val, IS_NON_TRANSLITERATION).trim();
        }    
    }
    
    /**
     * 
     * @param val
     * @return 
     */
    public static String getCumulusSimpleVal(String val){
        if(val.indexOf(LANG_SEPARATOR)>-1){
            return applyRules(val.substring(val.indexOf(LANG_SEPARATOR)+1
                    , val.length()), IS_RSS).trim();
        } else {
            return applyRules(val, IS_RSS).trim();
        }     
    }
    
    /**
     * 
     * @param val cumulus raw xml value felt.
     * @return true hvis &lt;&lt;the>> eksisterer ellers false.
     */
    public static boolean isCumulusValNonSort(String val){
        return val.matches("^.*<<[^=]*>>.*$");
    }
    
    
    /**
     * 
     * @param val Cumulus raw xml value felt.
     * @return Værdien, med cumulus regler overholdt.
     */
    public static String getCumulusValNonSort(String val){       
        if(val.matches("^.*<<[^=]*>>.*$")){
            return val.substring(val.indexOf("<<")+2, val.indexOf(">>"));
        } else {
            return val;
        }    
    }
    
    /**
     * 
     * @param val Cumulus raw xml value felt.
     * @return true hvis &lt;&lt;a=b>> eksisterer ellers false.
     */
    public static Boolean isCumulusValTranslit(String val){
        return val.matches("^.*<<.*=.*$");
    }
    
    /**
     * forudsætter at der er blevet testet med isCumulusValSubject.
     * @param val Cumulus raw xml value felt.
     * @return Det der står efter = i "&lt;&lt;a=b>>"
     */
    public static String getCumulusValTranslit(String val){
        if(val.indexOf(LANG_SEPARATOR)>-1){
            return applyRules(val.substring(val.indexOf(LANG_SEPARATOR)+1
                    , val.length()), IS_TRANSLITERATION_REX).trim();
        } else {
            return applyRules(val, IS_TRANSLITERATION_REX).trim();
        }
    }
    
    /**
     * 
     * @param val Streng der skal parses.
     * @param transliteration int der beskriver hvilken regel der skal 
     *  parses efter.
     * @return Parset streng
     */
    public static String applyRules(String val, int transliteration){
        if(val.matches("^.*<<[^=]*>>.*$") && transliteration != IS_RSS){
            val = val.replaceAll("<<[^=]*>>", "");
        }
        if (val.matches("^.*<<.*=.*$")) {
            switch (transliteration){
                case IS_NON_TRANSLITERATION: val = 
                    val.replaceAll("<<", "").replaceAll("=.*>>", "");
                    break;
                case IS_RSS: // Same as below.
                case IS_TRANSLITERATION_REX: val = 
                    val.replaceAll(">>", "").replaceAll("<<.*=", "");
                    break;
                default:break;
            }
        }
        return val;          
    }
    
    /**
     * 
     * @param val Cumulus raw xml value felt.
     * @param defaultlang sprog værdi der skal bruges hvis der ikke er :
     * @return rfc4646 Sprogkode
     */
    public static String getCumulusLang(String val, String defaultlang){
        if(val.indexOf(LANG_SEPARATOR)>-1){
            String langCode = val.substring(0, val.indexOf(LANG_SEPARATOR));  
            if(langCode.matches(
                    "^[a-z][a-z][a-z]?(-[a-z]{4}(-[1-9]{3}|-[a-zA-Z]{2})?)?$")){
                return langCode;
            } else{
                logger.warn("Sprogkode '" + langCode + "' er ikke rfc4646");
                return defaultlang;
            }
        } else {
            if(defaultlang.matches(
            "^[a-z][a-z][a-z]?(-[a-z]{4}(-[1-9]{3}|-[a-zA-Z]{2})?)?$")){
                return defaultlang;
            } else {
                logger.warn("Sprogkode '" + defaultlang + "' er ikke rfc4646");
                return defaultlang;
            }
        }    
    }
    
    /**
     * 
     * @param val
     * @param lang
     * @param defLang
     * @return
     */
    public static boolean isTocTitle(String val, String lang, String defLang){
        if(val.indexOf(LANG_SEPARATOR)>-1){
            if(val.startsWith(lang + LANG_SEPARATOR)){
                return true;
            }
            return false;
        } else if (lang.equals(defLang)){
            return true;
        } 
        return false;
        
    }


    public static String getIsoDate(String informat, String outformat, String indate) {
	java.text.SimpleDateFormat informatter = new java.text.SimpleDateFormat(informat);
	java.util.Date date = null;
	try {
	    date = informatter.parse(indate);
	} catch(java.text.ParseException parseError) {
	}
	if(date != null) {
	    java.text.SimpleDateFormat outformatter = new java.text.SimpleDateFormat(outformat);
	    return outformatter.format(date);
	} else {
	    return "0000-00-00";
	}
    }
    
    /**
     * 
     * @param val
     * @param lang
     * @return
     */
    public static String getTocTitle(String val, String lang){
        if(val.startsWith(lang + LANG_SEPARATOR)){
            return val.substring(val.indexOf(LANG_SEPARATOR)+1,val.length());
        } else {   
            return val;
        }
        
    }
}
