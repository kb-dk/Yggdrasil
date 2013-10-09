package dk.kb.yggdrasil;

import java.io.File;
import java.util.LinkedHashMap;

import dk.kb.yggdrasil.utils.YamlTools;

public class YamlToolsTester {

    /**
     * @param args
     * @throws Exception 
     */
   public static void main(String[] args) throws Exception {
            File f = new File("/home/svc/Yggdrasil/src/main/ressources/rabbitmq.yml");
            LinkedHashMap m = YamlTools.loadYamlSettings(f);
            for (Object o: m.keySet()) {
                System.out.println(o);
            }
            String mode = RunningMode.getMode().toString().toLowerCase();
            if (!m.containsKey(mode)) {
                System.out.println("No settings available for the chosen mode: " + mode);
            } else {
                LinkedHashMap modeMap = (LinkedHashMap) m.get(mode);
                
                for (Object o1: modeMap.keySet()) {
                    String prop = (String) o1;
                    System.out.println("prop: " + prop);
                    if (prop.equalsIgnoreCase(RabbitMqSettings.RABBIT_MQ_YAML_PROPERTY)) {
                        LinkedHashMap m1 = (LinkedHashMap)modeMap.get(prop);
                        RabbitMqSettings rmSetttings = new RabbitMqSettings(m1);
                        System.out.println("brokerURI:" + rmSetttings.getBrokerUri());
                        System.out.println("PreservationDestination:" + rmSetttings.getPreservationDestination());
                    }
                    
                    
                    
                    //System.out.println(modeMap.get((String)o1).getClass().getName());
                    //LinkedHashMap m1 = (LinkedHashMap)o1;
                    //for (Object o2: m1.keySet()) {
                    //    System.out.println(o2.getClass());
                    //}
                }   
                
            }
            
        }

    }


