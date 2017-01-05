
#include <caml/mlvalues.h>
typedef struct _obj_st {
	double d;
	int i;
	char c;
} obj_st;

typedef obj_st *obj_p;



CAMLprim value
wrapping_ptr_ml2c ( value d, value i , value c )
{
	obj_p my_obj;
	my_obj = malloc(sizeof(obj_st));
	my_obj->d = Double_val(d);
	my_obj->i - Int_val(i);
	my_obj->c = Int_val(c);
	return (value) my_obj;
}


CAMLprim value dump_ptr(value ml_ptr )
{
	obj_p my_obj;
	my_obj = (obj_p) ml_ptr;
	printf(" d: %g\n i: %d\n c: %c\n",
		my_obj->d,
		my_obj->i,
		my_obj->c );
	return Val_unit;
}

CAMLprim value free_ptr( value ml_ptr )
{
	obj_p my_obj;
	my_obj = (obj_p) ml_ptr;
	free(my_obj);
	return Val_unit;
}


