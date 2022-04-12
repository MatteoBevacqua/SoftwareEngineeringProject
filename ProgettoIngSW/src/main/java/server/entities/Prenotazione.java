package server.entities;

import server.DTOs.SeatDTO;

import java.io.Serializable;
import java.util.Collection;

public record Prenotazione(int id, Volo volo, Collection<SeatDTO> bookedSeats, Collection<SeatDTO> mySeats, int miglia, boolean canCheckIn) implements Serializable {


}
