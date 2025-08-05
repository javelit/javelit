import streamlit as st
import time

empty = st.empty()
for i in range(10):
    empty.button("click me")
    time.sleep(1)