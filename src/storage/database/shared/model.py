from sqlalchemy import BigInteger, DateTime, Identity, Index, Integer, JSON, PrimaryKeyConstraint, Text, text
from typing import Optional
import datetime

from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column

class Base(DeclarativeBase):
    pass