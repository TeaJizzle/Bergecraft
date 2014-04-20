package com.valadian.bergecraft.interfaces;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.valadian.bergecraft.Bergecraft;

public class ApiManager {
    private List<IDisabler> disablerApis;
	
	public ApiManager()
	{
		Bergecraft.info("Loading APIs");
		disablerApis = new ArrayList<IDisabler>();
		disablerApis.add(new CompatGimmickApi());
	}
	
	public boolean isBergecraftDisabledFor(Player player){
		boolean disabled = false;
		for(IDisabler disabler : disablerApis)
		{
			disabled |= disabler.isBergecraftDisabledFor(player);
		}
		return disabled;
	}
}
