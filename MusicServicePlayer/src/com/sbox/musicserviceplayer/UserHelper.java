package com.sbox.musicserviceplayer;


public class UserHelper 
{
	private static Boolean isExit = false;
	public final static String Path="path";
	public final static String Title="title";
	public final static String Duration="duration";
	public final static String Size="size";
	public final static String Artist="artist";
	public final static String Date="date";
	
	public static Boolean getIsExit() 
	{  
		return isExit;  
	}
	
	public static void setIsExit(Boolean isExit) 
	{  
	    UserHelper.isExit = isExit;  
	}
}
