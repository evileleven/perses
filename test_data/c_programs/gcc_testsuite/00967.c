



unsigned char *p = (unsigned char *) 0x7f14;

void
Do (void)
{
  (*((volatile unsigned char*)0x7f14)) |= 0x01;
}

int
main (void)
{
  *p = 0x34;
  Do ();
  return (*p == 0x35) ? 0 : 1;
}
