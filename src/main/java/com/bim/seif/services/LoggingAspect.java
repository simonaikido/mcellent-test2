package com.bim.seif.services;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

//@Slf4j
//@Aspect
//@Component
public class LoggingAspect {

//    @Pointcut("execution(* com.bim.seif.controllers..*(..))")
//    public void serviciosPointcut() {}
//
//    @Before("serviciosPointcut()")
//    public void logAntes(JoinPoint joinPoint) {
//        log.info("Entrando a: {} con argumentos: {}", joinPoint.getSignature(), joinPoint.getArgs());
//    }
//
//    @AfterReturning(pointcut = "serviciosPointcut()", returning = "resultado")
//    public void logDespues(JoinPoint joinPoint, Object resultado) {
//        log.info("Salida de: {} con resultado: {}", joinPoint.getSignature(), resultado);
//    }
//
//    @AfterThrowing(pointcut = "serviciosPointcut()", throwing = "ex")
//    public void logErrores(JoinPoint joinPoint, Throwable ex) {
//        log.error("Excepción en: {} - {}", joinPoint.getSignature(), ex.getMessage());
//    }
}