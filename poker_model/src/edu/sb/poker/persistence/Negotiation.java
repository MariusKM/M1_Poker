package edu.sb.poker.persistence;

import static javax.xml.bind.annotation.XmlAccessType.NONE;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import edu.sb.poker.util.JsonProtectedPropertyStrategy;

@Embeddable
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
@XmlAccessorType(NONE)
@XmlType @XmlRootElement
public class Negotiation implements Comparable<Negotiation>  {
	static private final Comparator<Negotiation> COMPARATOR = Comparator.comparing(Negotiation::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Negotiation::getOffer, Comparator.nullsLast(Comparator.naturalOrder()))
			.thenComparing(Negotiation::getAnswer, Comparator.nullsLast(Comparator.naturalOrder()));
	
	@Column(nullable = true, updatable = true)
	@Size(min=0, max=2046)
	private String offer;
	
	@Column(nullable = true, updatable = true)
	@Size(min=0, max=2046)
	private String answer;

	@Column(nullable = true, updatable = true)
	private Long timestamp;
	
	@JsonbProperty @XmlAttribute
	public String getOffer() {
		return offer;
	}
	
	public void setOffer(String offer) {
		this.offer = offer;
	}

	@JsonbProperty @XmlAttribute
	public String getAnswer() {
		return answer;
	}
	
	public void setAnswer(String answer) {
		this.answer = answer;
	}

	@JsonbProperty @XmlAttribute
	public Long getTimestamp() {
		return timestamp;
	}
	
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(Negotiation other) {
		return COMPARATOR.compare(this,other);
	}

	

}