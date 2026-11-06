package com.std.ec.PrimerSpring.handler;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
/* cual es el objetivo no sobre cargar la pagina con redireccionamientos a otro html solo poner sobre escritura
no cambia el path lo sobre escribe */
@Controller 
/*errocontroller sprint web pertence a mvc  */
public class Custom404 implements ErrorController {
    /*metodo de redirecionamiento a la pagina 404 de sobre escritura, NO mas de en la ruta */
    @RequestMapping ("/error")
    public String getErrorPath(){
        return "/404.xhtml";
    }
}
