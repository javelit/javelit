import streamlit as st
import time

st.text("something")
for i in range(10):
    time.sleep(1)
    st.text("coucou Louise")
