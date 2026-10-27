package dev.ofarafal.shizucleaner;

interface IShellService {
    // Transakcja zarezerwowana przez Shizuku dla UserService.
    void destroy() = 16777114;

    // Zwraca "EXIT:<kod>\n<stdout+stderr>". Serwis przyjmuje wyłącznie polecenia z allowlisty.
    String exec(in String[] command) = 1;
}
