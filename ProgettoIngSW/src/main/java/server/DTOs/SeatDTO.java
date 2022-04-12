package server.DTOs;

import java.io.Serializable;

public record SeatDTO(int seatId, boolean isBooked, boolean bookedByMe,
                      float prezzo) implements Serializable, Comparable<SeatDTO> {


    @Override
    public int compareTo(SeatDTO o) {
        return Integer.compare(this.seatId, o.seatId);
    }

    @Override
    public int hashCode() {
        return this.seatId;
    }
}
