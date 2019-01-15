/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.abehrdigital.dicomprocessor;

/**
 *
 * @author admin
 */
public interface DaoCommand {

    public Object execute(DaoManager daoManager);
}
