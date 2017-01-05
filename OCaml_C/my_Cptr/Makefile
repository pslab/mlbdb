wrap.o: wrap.c
	ocamlc -c $<

dllwrap_stubs.so: wrap.o
	ocamlmklib  -o  wrap_stubs  $<

funs.mli: funs.ml
	ocamlc -i $< > $@

funs.cmi: funs.mli
	ocamlc -c $<

funs.cmo: funs.ml funs.cmi
	ocamlc -c $<

funs.cma:  funs.cmo  dllwrap_stubs.so
	ocamlc -a  -o $@  $<  -dllib -lwrap_stubs

funs.cmx: funs.ml funs.cmi
	ocamlopt -c $<

funs.cmxa:  funs.cmx  dllwrap_stubs.so
	ocamlopt -a  -o $@  $<  -cclib -lwrap_stubs

clean:
	rm -f *.[oa] *.so *.cm[ixoa] *.cmxa
