package fr.acore.bungeecord.api.config;

public interface IStringHelper {
	
	public default String replace(String chaine, String regex, String data) {
		chaine = chaine.replace(regex, data);
		return chaine;
	}

}
