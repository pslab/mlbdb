type t
external abs_get: float -> int -> char -> t = "wrapping_ptr_ml2c"
external print_t: t -> unit = "dump_ptr"
external free_t : t -> unit = "free_ptr"

let ty = abs_get 255.9 107 'K'
let () = print_t ty;
(*	free_t ty;*)
;;
