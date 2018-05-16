/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.mackenzie.cd.relogio;

import br.mackenzie.cd.udp.UDPServer;
import br.mackenzie.cd.udp.UDPClient;

/**
 *
 * @author 31416489
 */
public class Relogio {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        if (args.length < 1) {
            System.out.println("Usage: java -jar RelogioUDP.jar <-m|-s>");
            return;
        }
        
        if (args[0].equals("-m")) {
            UDPServer.start(args);
        } else if (args[0].equals("-s")) {
            UDPClient.start(args);
        } else {
            System.out.println("Usage: java -jar RelogioUDP.jar <-m|-s>");
        }
    }
    
}
