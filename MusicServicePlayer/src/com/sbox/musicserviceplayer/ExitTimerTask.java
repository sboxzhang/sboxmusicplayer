package com.sbox.musicserviceplayer;

import java.util.TimerTask;

import com.sbox.musicserviceplayer.UserHelper;

public class ExitTimerTask extends TimerTask
{
	@Override
	public void run()
	{
		UserHelper.setIsExit(false);
	}
}