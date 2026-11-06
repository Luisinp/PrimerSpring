package com.std.ec.PrimerSpring.controller;

import java.io.Serializable;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.faces.view.ViewScoped;

@Component("loginMB")
@ViewScoped 
public class loginbean implements Serializable {

    // Renombrado a getUsername para coincidir con #{loginMB.username}
    public String getUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth == null || !auth.isAuthenticated()) {
            return "Invitado";
        }

        Object datoUser = auth.getPrincipal();
        
        if (datoUser instanceof UserDetails) {
            return ((UserDetails) datoUser).getUsername();
        } else if (datoUser != null) {
            return datoUser.toString();
        }

        return "Invitado";
    }
}



/* package com.std.ec.PrimerSpring.controller;

import java.io.Serializable;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.faces.view.ViewScoped;

@Component("loginMB")
@ViewScoped 
public class loginbean implements Serializable {
    public String getUserName(){
        Object datoUser = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(datoUser instanceof UserDetails){
            return((UserDetails)datoUser).getUsername();
        }
        return datoUser.toString();
    }

}
*/