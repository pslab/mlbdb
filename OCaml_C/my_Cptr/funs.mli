type t
external abs_get : float -> int -> char -> t = "wrapping_ptr_ml2c"
external print_t : t -> unit = "dump_ptr"
external free_t : t -> unit = "free_ptr"
val ty : t
