package dev.ofarafal.shizucleaner;

interface IShellService {
    // Transakcja zarezerwowana przez Shizuku dla UserService.
    void destroy() = 16777114;

    // Zwraca "EXIT:<kod>\n<stdout+stderr>". Tylko polecenia z CommandPolicy.
    String exec(in String[] command) = 1;

    // Skany plików w pamięci współdzielonej (patrz Wire). Zwraca linie rozdzielone tabulatorami.
    String scan(String kind, in String[] args) = 2;

    // Kasowanie ścieżek z opcjonalnym nadpisaniem (passes 0..3). Zwraca linie OK/ERR.
    String delete(in String[] paths, int passes) = 3;
}
