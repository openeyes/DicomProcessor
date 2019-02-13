/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor.utils;

/**
 * @author admin
 */
public enum Status {
    NEW(),
    RETRY(),
    COMPLETE(),
    FAILED(),
    PAUSE(),
    VOID()
}

