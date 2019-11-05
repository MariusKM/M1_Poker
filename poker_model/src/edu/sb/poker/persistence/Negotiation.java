package edu.sb.poker.persistence;

import java.util.Comparator;

import javax.json.bind.annotation.JsonbVisibility;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.Size;

import edu.sb.poker.util.JsonProtectedPropertyStrategy;

@Embeddable
@JsonbVisibility(JsonProtectedPropertyStrategy.class)
public class Negotiation implements Comparable<Negotiation>  {
	static private final Comparator<Negotiation> COMPARATOR = Comparator.comparing(Negotiation::getTimestamp).thenComparing(Negotiation::getOffer);
	@Column(nullable = true, updatable = true)
	@Size(min=0, max=2046)
	private String offer;
	
	@Column(nullable = true, updatable = true)
	@Size(min=0, max=2046)
	private String answer;

	@Column(nullable = true, updatable = true)
	private Long timestamp;

	public String getOffer() {
		return offer;
	}
	
	protected void setOffer(String offer) {
		this.offer = offer;
	}

	public String getAnswer() {
		return answer;
	}
	
	protected void setAnswer(String answer) {
		this.answer = answer;
	}

	public Long getTimestamp() {
		return timestamp;
	}
	
	protected void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(Negotiation other) {
		return COMPARATOR.compare(this,other);
	}

	

}

