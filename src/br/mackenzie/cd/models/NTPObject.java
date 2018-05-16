/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.mackenzie.cd.models;

import java.net.InetAddress;

/**
 *
 * @author 31416489
 */
public class NTPObject {
    public InetAddress IPAddress;
    public int port;
    public Long time;

    public NTPObject(InetAddress IPAddress, int port) {
        this.IPAddress = IPAddress;
        this.port = port;
        this.time = null;
    }
    
}
