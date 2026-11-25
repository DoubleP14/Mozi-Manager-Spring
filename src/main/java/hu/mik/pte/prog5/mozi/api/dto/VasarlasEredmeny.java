package hu.mik.pte.prog5.mozi.api.dto;

import java.util.List;

// A sikeres vasarlas osszefoglaloja, a pontszam mar a trigger altal jovairt ertek
public record VasarlasEredmeny(List<Long> jegyIdk, List<Integer> szekek, int egysegar, int osszeg,
                               int torzsvasarloiPontok) {
}
