#!/bin/bash
javac -cp ".:./gson-2.8.5.jar" sharedClasses/*.java ;
javac -cp ".:./gson-2.8.5.jar" client/*.java ;
javac -cp ".:./gson-2.8.5.jar" server/*.java ;
