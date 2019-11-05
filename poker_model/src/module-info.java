module poker_model {
	requires transitive java.logging;
	requires transitive java.validation;
	requires transitive javax.annotation.api;

	requires transitive java.xml.bind;
	requires transitive java.json.bind;
	requires transitive java.ws.rs;
	requires transitive javax.persistence;
	requires transitive eclipselink.annotation;

	opens edu.sb.poker.persistence;
	exports edu.sb.poker.persistence;
	exports edu.sb.poker.service;
	exports edu.sb.poker.util;
}